#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <errno.h>
#include <string.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "cr.h"
#include "list.h"
#include "syscalls.h"

// Number of fds that we allow for the function to use. We may use some fds after this limit.
#define RESERVED_FDS 768

// This variable is used to generate reserved fds. Used for:
// - meta snapshot fd;
// - memory snapshot fd;
// - seccomp fd.
int next_reserved_fd = RESERVED_FDS;

// Tags used in the meta snapshot fd.
#define MEMORY_TAG  -3
#define ABI_TAG     -2
#define ISOLATE_TAG -1

// Serialized memory struct.
typedef struct {
    // Start address of the memory segment.
    void* addr;
    // Length of the memory segment in bytes.
    size_t length;
    // Permissions of the memory segment.
    int prot;
    // Population count (used to verify integrity).
    size_t pop;
} memory_t;

size_t memory_to_file(int fd, char* buffer, size_t count) {
    size_t written = 0;
    size_t n;
    while (written < count) {
        if ((n = write(fd, buffer + written, count - written)) < 0) {
            if (errno == EINTR || errno == EAGAIN) {
                continue;
            } else {
                err("error: could not write data\n");
            }
        }
        written += n;
    }
    return written;
}

size_t file_to_memory(int fd, char* buffer, size_t count) {
    size_t written = 0;
    size_t n;
    while (written < count) {
        if ((n = read(fd, buffer + written, count - written)) < 0) {
            if (errno == EINTR || errno == EAGAIN) {
                continue;
            } else {
                err("error: could not read data\n");
            }
        }
        written += n;
    }
    return written;
}

int move_fd(int oldfd, int newfd) {
    if (oldfd == newfd) {
        return oldfd;
    } else {
        int dupped = dup2(oldfd, newfd);
        if (dupped < 0) {
            perror("error: failed to move fd using dup2");
            return oldfd;
        } else {
            close(oldfd);
            return newfd;
        }
    }
}

int move_to_reserved_fd(int oldfd) {
    return move_fd(oldfd, __atomic_fetch_add(&next_reserved_fd, 1, __ATOMIC_SEQ_CST));
}

size_t popcount(char* buffer, size_t count) {
    size_t result = 0;
    for (int i = 0; i < count / sizeof(unsigned int); i++) {
        result += __builtin_popcount(*(unsigned int*)buffer);
        buffer += sizeof(unsigned int);
    }
    return result;
}

void print_proc_maps(char* filename) {
    char buffer[512];

    FILE* pmaps = fopen("/proc/self/maps", "r");
    if (pmaps == NULL) {
        err("error: failed to open proc self maps\n");
        return;
    }

    FILE* out = fopen(filename, "w");
    if (out == NULL) {
        err("error: failed to open %s\n", filename);
        return;
    }

    while (fgets(buffer, sizeof(buffer), pmaps)) {
        unsigned long start, finish, offset, inode, pop;
        char r, w, x, p;
        char dev[16];
        char mpath[256];

        // Resetting vars.
        dev[0] = '\0';
        mpath[0] = '\0';

        int matched = sscanf(buffer, "%lx-%lx %c%c%c%c %lx %15s %lu %255s",
            &start, &finish, &r, &w, &x, &p, &offset, dev, &inode, mpath);
        if (matched < 9) {
            fprintf(out, "matched = %d on %s\n", matched, buffer);
        }

        // If we can't read or if we are looking into vvar/vdso, ignore.
        if (r == '-' || !strcmp(mpath, "[vvar]") || !strcmp(mpath, "[vdso]")) {
            pop = 0;
        } else {
            pop = popcount((void*)start, finish - start);
        }

        fprintf(out, "%16p - %16p %c%c%c%c off=%8lu sz=0x%10lx pop=%10lu %s\n",
            (void*)start,
            (void*) finish,
            r, w, x, p,
            offset,
            finish - start,
            pop,
            mpath);
    }

    fclose(pmaps);
    fclose(out);
}

// A memory seg is a set of contiguous memory pages with the same permission.
void checkpoint_memory_segment(void* seg_start, void* seg_finish, char seg_perm, int memsnap_fd, int metasnap_fd) {
    int tag = MEMORY_TAG;
    size_t seg_size = (char*) seg_finish - (char*) seg_start;
    size_t pop = popcount(seg_start, seg_size);
    dbg("cmemory:  %16p - %16p size = 0x%16lx prot = %s%s%s%s popcount = %lu\n",
        seg_start,
        seg_finish,
        seg_size,
        seg_perm & PROT_EXEC   ? "x" : "-",
        seg_perm & PROT_READ   ? "r" : "-",
        seg_perm & PROT_WRITE  ? "w" : "-",
        seg_perm == PROT_NONE  ? "n" : "-",
        pop);

    // Write contents to file.
    memory_to_file(memsnap_fd, seg_start, seg_size);

    // Write metadata tag.
    if (write(metasnap_fd, &tag, sizeof(int)) != sizeof(int)) {
        perror("error: failed to serialize memory tag");
    }

    // Write metadata content.
    memory_t s = {.addr = seg_start, .length = seg_size, .prot = seg_perm, .pop = pop};
    if (write(metasnap_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
        perror("error: failed to serialize memory header");
    }
}

void checkpoint_dirty_block(void* map_start, void* map_finish, void* block_start, void* block_finish, char* perms, int memsnap_fd, int metasnap_fd) {
    void* seg_start = block_start;

    // Within a single dirty block we might have several memory permissions so we need to iterate these segments.
     while (seg_start < block_finish) {
        char  seg_perm = permission(map_start, map_finish, perms, seg_start);
        char* seg_finish = repeated(map_start, map_finish, perms, seg_start);
        checkpoint_memory_segment(seg_start, seg_finish, seg_perm, memsnap_fd, metasnap_fd);
        seg_start = seg_finish;
    }
}

void checkpoint_memory_mapping(void* map_start, void* map_finish, char* map_perms, char* map_dirty, int memsnap_fd, int metasnap_fd) {
    void* block_start = map_start;

    while (block_start < map_finish) {
        char  block_dirty = permission(map_start, map_finish, map_dirty, block_start);
        char* block_finish = repeated(map_start, map_finish, map_dirty, block_start);
        if (block_dirty == PROT_WRITE) {
            checkpoint_dirty_block(map_start, map_finish, block_start, block_finish, map_perms, memsnap_fd, metasnap_fd);
        }
        block_start = block_finish;
    }
}

void checkpoint_mappings(int meta_snap_fd, int mem_snap_fd, mapping_t* mappings) {
    mapping_t* current = mappings;

    // If the list if empty, do nothing.
    if (current->start == NULL) {
        return;
    }

    while (current != NULL) {
        void* mapping_start = current->start;
        void* mapping_finish = ((char*) mapping_start) + current->size;
        checkpoint_memory_mapping(mapping_start, mapping_finish, current->permissions, current->dirty, mem_snap_fd, meta_snap_fd);
        current = current->next;
    }
}

void restore_memory(int meta_snap_fd, int mem_snapshot_fd) {
    memory_t s;

    if (read(meta_snap_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
        perror("error: failed to deserialize memory header");
    }

#ifdef OPT
    off_t offset = lseek(mem_snapshot_fd, 0, SEEK_CUR);
    if (offset == -1) {
        perror("error: failed to get offset with lseek");
    }

    void* ret = mmap(s.addr, s.length, s.prot, MAP_PRIVATE | MAP_FIXED, mem_snapshot_fd, offset);
    if (s.addr != ret) {
        err("error: failed to replay mmap:\t expected = %16p got = %16p\n",  s.addr, ret);
    }

    if (lseek(mem_snapshot_fd, s.length, SEEK_CUR) == -1) {
        perror("error: failed to advance offset with lseek");
    }
#else
    // We might need to restore memory into a range that is not writeable.
    if (!(s.prot & PROT_WRITE)) {
        if (mprotect(s.addr, s.length, PROT_WRITE)) {
            perror("error: failed to mprotect before loading snap into memory");
        }
    }

    file_to_memory(mem_snapshot_fd, (char*)s.addr, s.length);

    // Restore the original permissions after restoring memory from snapshot.
    if (!(s.prot & PROT_WRITE)) {
        if (mprotect(s.addr, s.length, s.prot)) {
            perror("error: failed to mprotect after loading snap into memory");
        }
    }
#endif

#ifdef DEBUG
    // We are checking the the population count matches.
    if (s.pop != popcount(s.addr, s.length)) {
        err("error: popcount for %16p - %16p doesn't match: before checkpoint = %lu after restore = %lu\n",
            s.addr, ((char*) s.addr) + s.length, s.pop, popcount(s.addr, s.length));
    }
#endif

    dbg("rmemory:  %16p - %16p size = 0x%16lx prot = %s%s%s%s popcount = %lu\n",
        s.addr,
        ((char*) s.addr) + s.length,
        s.length,
        s.prot & PROT_EXEC   ? "x" : "-",
        s.prot & PROT_READ   ? "r" : "-",
        s.prot & PROT_WRITE  ? "w" : "-",
        s.prot == PROT_NONE  ? "n" : "-",
        s.pop);
}

void checkpoint_isolate(int meta_snap_fd, graal_isolate_t* isolate) {
    int tag = ISOLATE_TAG;
    dbg("isolate: %16p\n", isolate);
    if (write(meta_snap_fd, &tag, sizeof(int)) != sizeof(int)) {
        perror("error: failed to serialize isolate tag");
    }
    if (write(meta_snap_fd, &isolate, sizeof(void*)) != sizeof(void*)) {
        perror("error: failed to serialize isolate pointer");
    }
}

void restore_isolate(int meta_snap_fd, graal_isolate_t** isolate) {
    if (read(meta_snap_fd, isolate, sizeof(void*)) != sizeof(void*)) {
        perror("error: failed to deserialize isolate pointer");
        return;
    }
    dbg("isolate: %16p\n", *isolate);
}

void print_abi(isolate_abi_t* abi) {
    dbg("abi.graal_create_isolate:    %16p\n", abi->graal_create_isolate);
    dbg("abi.graal_tear_down_isolate: %16p\n", abi->graal_tear_down_isolate);
    dbg("abi.entrypoint:              %16p\n", abi->entrypoint);
    dbg("abi.graal_detach_thread:     %16p\n", abi->graal_detach_thread);
    dbg("abi.graal_attach_thread:     %16p\n", abi->graal_attach_thread);
}

void checkpoint_abi(int meta_snap_fd, isolate_abi_t* abi) {
    int tag = ABI_TAG;
    print_abi(abi);
    if (write(meta_snap_fd, &tag, sizeof(int)) != sizeof(int)) {
        perror("error: failed to serialize abi tag");
    }
    if (write(meta_snap_fd, abi, sizeof(isolate_abi_t)) != sizeof(isolate_abi_t)) {
        perror("error: failed to serialize function abi struct");
    }
}

void restore_abi(int meta_snap_fd, isolate_abi_t* abi) {
    if (read(meta_snap_fd, abi, sizeof(isolate_abi_t)) != sizeof(isolate_abi_t)) {
        perror("error: failed to deserialize function abi struct");
        return;
    }
    print_abi(abi);
}

void checkpoint_syscall(int meta_snap_fd, int tag, void* syscall_args, size_t size) {
    if (write(meta_snap_fd, &tag, sizeof(int)) != sizeof(int)) {
        perror("error: failed to serialize syscall tag");
    }
    if (write(meta_snap_fd, syscall_args, size) != size) {
        perror("error: failed to serialize syscall arguments");
    }
}

void restore_mmap(int meta_snap_fd) {
    mmap_t s;
    void* ret;

    if (read(meta_snap_fd, &s, sizeof(mmap_t)) != sizeof(mmap_t)) {
        perror("error: failed to deserialize mmap arguments");
    }

    print_mmap(&s);

    // We use the previously returned address and MAP_FIXED to ensure that we recover the correct address.
    ret = (void*) syscall(__NR_mmap, s.ret, s.length, s.prot, s.flags | MAP_FIXED, s.fd, s.offset);
    if (s.ret != ret) {
        err("error: failed to replay mmap:\t original ret = %16p got ret = %16p\n",  s.ret, ret);
    }
}

void restore_munmap(int meta_snap_fd) {
    munmap_t s;
    int ret;

    if (read(meta_snap_fd, &s, sizeof(munmap_t)) != sizeof(munmap_t)) {
        perror("error: failed to deserialize munmap arguments");
    }

    print_munmap(&s);

    ret = syscall(__NR_munmap, s.addr, s.length);
    if (s.ret != ret) {
        err("error: failed to replay munmap:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void restore_mprotect(int meta_snap_fd) {
    mprotect_t s;
    int ret;

    if (read(meta_snap_fd, &s, sizeof(mprotect_t)) != sizeof(mprotect_t)) {
        perror("error: failed to deserialize mprotect arguments");
    }

    print_mprotect(&s);

    ret = syscall(__NR_mprotect, s.addr, s.length, s.prot);
    if (s.ret != ret) {
        err("error: failed to replay mprotect:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void restore_dup(int meta_snap_fd) {
    dup_t s;
    int ret;

    if (read(meta_snap_fd, &s, sizeof(dup_t)) != sizeof(dup_t)) {
        perror("error: failed to deserialize dup arguments");
    }

    print_dup(&s);

    ret = syscall(__NR_dup2, s.oldfd, s.ret);
    if (s.ret != ret) {
        err("error: failed to replay dup:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void restore_dup2(int meta_snap_fd) {
    dup2_t s;
    int ret;

    if (read(meta_snap_fd, &s, sizeof(dup2_t)) != sizeof(dup2_t)) {
        perror("error: failed to deserialize dup arguments");
    }

    print_dup2(&s);

    ret = syscall(__NR_dup2, s.oldfd, s.newfd);
    if (s.ret != ret) {
        err("error: failed to replay dup2:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void restore_open(int meta_snap_fd) {
    open_t s;
    int ret;

    if (read(meta_snap_fd, &s, sizeof(open_t)) != sizeof(open_t)) {
        perror("error: failed to deserialize open arguments");
    }

    print_open(&s);

    ret = syscall(__NR_open, s.pathname, s.flags, s.mode);
    if (s.ret != ret) {
        // If the file descriptor picked by open is not the expected, try to move it.
        ret = move_fd(ret, s.ret);
        if (s.ret != ret) {
            err("error: failed to replay open:\t original ret = %d got ret = %d\n",  s.ret, ret);
        }
    }
}

void restore_openat(int meta_snap_fd) {
    openat_t s;
    int ret;

    if (read(meta_snap_fd, &s, sizeof(openat_t)) != sizeof(openat_t)) {
        perror("error: failed to deserialize openat arguments");
    }

    print_openat(&s);

    ret = syscall(__NR_openat, s.dirfd, s.pathname, s.flags, s.mode);
    if (s.ret != ret) {
        // If the return does not match but was not an error, try to save it by moving the fd.
        if (ret >= 0) {
            ret = move_fd(ret, s.ret);
        }
        if (s.ret != ret) {
            err("error: failed to replay openat:\t original ret = %d got ret = %d\n",  s.ret, ret);
        }
    }
}

void restore_close(int meta_snap_fd) {
    close_t s;

    if (read(meta_snap_fd, &s, sizeof(close_t)) != sizeof(close_t)) {
        perror("error: failed to deserialize close arguments");
    }

    print_close(&s);

    // Note: we don't check the value of close because we are not tracking other syscalls that could
    // leave open file descriptors (e.g., socket).
    syscall(__NR_close, s.fd);
}

void checkpoint_memory(int meta_snap_fd, int mem_snap_fd, mapping_t* mappings, isolate_abi_t* abi, graal_isolate_t* isolate) {
    checkpoint_mappings(meta_snap_fd, mem_snap_fd, mappings);
    checkpoint_abi(meta_snap_fd, abi);
    checkpoint_isolate(meta_snap_fd, isolate);
}

void restore(const char* meta_snap_path, const char* mem_snap_path, isolate_abi_t* abi, graal_isolate_t** isolate){
    // Open the metadata file (syscall arguments, memory ranges, etc).
    int meta_snap_fd = open(meta_snap_path, O_RDONLY);
    if (meta_snap_fd < 0) {
        perror("error: failed to open meta snapshot file");
    } else {
        meta_snap_fd = move_to_reserved_fd(meta_snap_fd);
    }

    // Open the memory snapshot file.
    int mem_snap_fd = open(mem_snap_path, O_RDONLY);
    if (mem_snap_fd < 0) {
        perror("error: failed to open memory snapshot file");
    } else {
        mem_snap_fd = move_to_reserved_fd(mem_snap_fd);
    }

    while(1) {
        int tag;
        size_t n = read(meta_snap_fd, &tag, sizeof(int));

        if (n == 0) {
            break;
        } else if (n != sizeof(int)) {
            perror("error: failed to read tag");
        }

        switch (tag)
        {
        case __NR_mmap:
            restore_mmap(meta_snap_fd);
            break;
        case __NR_munmap:
            restore_munmap(meta_snap_fd);
            break;
        case __NR_mprotect:
            restore_mprotect(meta_snap_fd);
            break;
        case __NR_dup:
            restore_dup(meta_snap_fd);
            break;
        case __NR_dup2:
            restore_dup2(meta_snap_fd);
            break;
        case __NR_open:
            restore_open(meta_snap_fd);
            break;
        case __NR_openat:
            restore_openat(meta_snap_fd);
            break;
        case __NR_close:
            restore_close(meta_snap_fd);
            break;
        case ISOLATE_TAG:
            restore_isolate(meta_snap_fd, isolate);
            break;
        case ABI_TAG:
            restore_abi(meta_snap_fd, abi);
            break;
        case MEMORY_TAG:
            restore_memory(meta_snap_fd, mem_snap_fd);
            break;
        default:
            err("error: unknown tag durin restore: %d", tag);
        }
    }

    close(mem_snap_fd);
    close(meta_snap_fd);
}

int load_function(const char* function_path, isolate_abi_t* abi) {
    char* derror = NULL;

    // Load function library.
    void* dhandle = dlopen(function_path, RTLD_LAZY);
    if (dhandle == NULL) {
        err("error: failed to load dynamic library: %s\n", dlerror());
        return 1;
    }

    // Load function abi.
    abi->graal_create_isolate = (int (*)(graal_create_isolate_params_t*, graal_isolate_t**, graal_isolatethread_t**)) dlsym(dhandle, "graal_create_isolate");
    if ((derror = dlerror()) != NULL) {
        err("error: %s\n", derror);
        return 1;
    }

    abi->graal_tear_down_isolate = (int (*)(graal_isolatethread_t*)) dlsym(dhandle, "graal_tear_down_isolate");
    if ((derror = dlerror()) != NULL) {
        err("error: %s\n", derror);
        return 1;
    }

    abi->entrypoint = (void (*)(graal_isolatethread_t*)) dlsym(dhandle, "entrypoint");
    if ((derror = dlerror()) != NULL) {
        err("error: %s\n", derror);
        return 1;
    }

    abi->graal_detach_thread = (int (*)(graal_isolatethread_t*)) dlsym(dhandle, "graal_detach_thread");
    if ((derror = dlerror()) != NULL) {
        err("error: %s\n", derror);
        return 1;
    }

    abi->graal_attach_thread = (int (*)(graal_isolate_t*, graal_isolatethread_t**)) dlsym(dhandle, "graal_attach_thread");
    if ((derror = dlerror()) != NULL) {
        err("error: %s\n", derror);
        return 1;
    }

    return 0;
}