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

#define MEMORY_TAG  -2
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
                fprintf(stderr, "Could not write() data\n");
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
                fprintf(stderr, "Could not read() data\n");
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
            perror("failed to move fd using dup2");
            return oldfd;
        } else {
            close(oldfd);
            return newfd;
        }
    }
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
        fprintf(stderr, "failed to open proc self maps\n");
        return;
    }

    FILE* out = fopen(filename, "w");
    if (out == NULL) {
        fprintf(stderr, "failed to open %s\n", filename);
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

int check_filepath_fd(int fd, char* path) {
    char fdpath[512];
    char filepath[512];
    sprintf(fdpath, "/proc/self/fd/%d", fd);

    size_t bytes = readlink(fdpath, filepath, sizeof(fdpath));

    // Note: readlink does not place a terminating null byte.
    filepath[bytes] = '\0';
    return strcmp(path, filepath);
}

int find_fd_filepath(char* path) {
    for (int i = 0; i < 8; i++) {
        if (!check_filepath_fd(i, path)) {
            return i;
        }
    }
    return -1;
}

// A memory seg is a set of contiguous memory pages with the same permission.
void checkpoint_memory_segment(void* seg_start, void* seg_finish, char seg_perm, int memsnap_fd, int metasnap_fd) {
    int tag = MEMORY_TAG;
    size_t seg_size = (char*) seg_finish - (char*) seg_start;
    size_t pop = popcount(seg_start, seg_size);
    fprintf(stderr, "cmemory:  %16p - %16p size = 0x%16lx prot = %s%s%s%s popcount = %lu\n",
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
        perror("failed to serialize memory tag");
    }

    // Write metadata content.
    memory_t s = {.addr = seg_start, .length = seg_size, .prot = seg_perm, .pop = pop};
    if (write(metasnap_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
        perror("failed to serialize memory header");
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

void checkpoint_memory_mappings(struct function_args* fargs, int memsnap_fd) {
    mapping_t* current = &(fargs->mappings);

    // If the list if empty, do nothing.
    if (current->start == NULL) {
        return;
    }

    while (current != NULL) {
        void* mapping_start = current->start;
        void* mapping_finish = ((char*) mapping_start) + current->size;
        checkpoint_memory_mapping(mapping_start, mapping_finish, current->permissions, current->dirty, memsnap_fd, fargs->meta_snapshot_fd);
        current = current->next;
    }
}

void checkpoint_memory_library(struct function_args* fargs, int memsnap_fd) {
    char buffer[512];
    int tag = MEMORY_TAG;
    unsigned long start, finish, prevfinish;
    char r, w, x, p;
    FILE* file = fopen("/proc/self/maps", "r");
    if (file == NULL) {
        fprintf(stderr, "failed to open proc self maps\n");
        return;
    }

    while (fgets(buffer, sizeof(buffer), file)) {
        // Reading start and finish addresses
        sscanf(buffer, "%lx-%lx %c%c%c%c", &start, &finish, &r, &w, &x, &p);

        // If the proc map line does not contain our function library AND
        // this map line is not a continuation of a previous mapping.
        if (strstr(buffer, fargs->function_path) == NULL && prevfinish != 0 && prevfinish != start) {
            continue;
        } else {
            size_t size = finish - start;

            // We ignore mappings of the library that were mmapped by the function.
            if (list_find(&(fargs->mappings), (void*) start, size) != NULL) {
                continue;
            }

            if (w == '-') {
                continue; // TODO - should we skip read/execute-only pages?
            }

            // Calculate population count.
            size_t pop = popcount((char*)start, size);

            fprintf(stderr, "clibrary: %16p - %16p size = 0x%16lx prot = %c%c%c%c popcount = %lu \n",
                (void*) start, ((char*) start) + size, size, r, w, x, p, pop);

            // Write contents to file.
            memory_to_file(memsnap_fd, (char*)start, size);

            // Write metadata tag.
            if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
                perror("failed to serialize memory tag");
            }
            // Write metadata content.
            memory_t s = {.addr = (void*)start, .length = size, .prot = 0, .pop = pop};
            s.prot = r == 'r' ? s.prot | PROT_READ : s.prot;
            s.prot = w == 'w' ? s.prot | PROT_WRITE : s.prot;
            s.prot = x == 'x' ? s.prot | PROT_EXEC : s.prot;
            // Note: we are not passing the 'p' permission.
            if (write(fargs->meta_snapshot_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
                perror("failed to serialize memory header");
            }

            prevfinish = finish;
        }
    }
    fclose(file);
}

void checkpoint_memory(struct function_args* fargs) {
    int memsnap_fd = open("memory.snap",  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR);
    if (memsnap_fd < 0) {
        fprintf(stderr, "failed to open memory.snap file\n");
        return;
    }

    // Checkpoint memory where the function dynamic library is installed.
    checkpoint_memory_library(fargs, memsnap_fd);

    // Checkpoint memory that hte function allocated after initialization.
    checkpoint_memory_mappings(fargs, memsnap_fd);

    close(memsnap_fd);
}

void restore_memory(struct function_args* fargs, int mem_snapshot_fd) {
    memory_t s;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
        perror("failed to deserialize memory header");
    }

    // We might need to restore memory into a range that is not writeable.
    if (!(s.prot & PROT_WRITE)) {
        if (mprotect(s.addr, s.length, PROT_WRITE)) {
            perror("failed to mprotect before loading snap into memory");
        }
    }

    file_to_memory(mem_snapshot_fd, (char*)s.addr, s.length);

    // Restore the original permissions after restoring memory from snapshot.
    if (!(s.prot & PROT_WRITE)) {
        if (mprotect(s.addr, s.length, s.prot)) {
            perror("failed to mprotect after loading snap into memory");
        }
    }

    // We are checking the the population count matches.
    if (s.pop != popcount(s.addr, s.length)) {
        fprintf(stderr, "error, popcount for %16p - %16p doesn't match: before checkpoint = %lu after restore = %lu\n",
            s.addr, ((char*) s.addr) + s.length, s.pop, popcount(s.addr, s.length));
    }

    fprintf(stderr, "rmemory:  %16p - %16p size = 0x%16lx prot = %s%s%s%s popcount = %lu\n",
        s.addr,
        ((char*) s.addr) + s.length,
        s.length,
        s.prot & PROT_EXEC   ? "x" : "-",
        s.prot & PROT_READ   ? "r" : "-",
        s.prot & PROT_WRITE  ? "w" : "-",
        s.prot == PROT_NONE  ? "n" : "-",
        s.pop);
}

void checkpoint_isolate(struct function_args* fargs, void* isolate) {
    int tag = ISOLATE_TAG;
    fprintf(stderr, "cisolate: %16p\n", isolate);
    if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
        perror("failed to serialize isolate tag");
    }
    if (write(fargs->meta_snapshot_fd, &isolate, sizeof(void*)) != sizeof(void*)) {
        perror("failed to serialize isolate pointer");
    }
}

void* restore_isolate(struct function_args* fargs) {
    void* isolate;
    if (read(fargs->meta_snapshot_fd, &isolate, sizeof(void*)) != sizeof(void*)) {
        perror("failed to deserialize isolate pointer");
        return NULL;
    }
    fprintf(stderr, "restore isolate: %16p\n", isolate);
    return isolate;
}

void checkpoint_syscall(struct function_args* fargs, int tag, void* syscall_args, size_t size) {
    if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
        perror("failed to serialize syscall tag");
    }
    if (write(fargs->meta_snapshot_fd, syscall_args, size) != size) {
        perror("failed to serialize syscall arguments");
    }
}

void restore_mmap(struct function_args* fargs) {
    mmap_t s;
    void* ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(mmap_t)) != sizeof(mmap_t)) {
        perror("failed to deserialize mmap arguments");
    }

    if (s.fd != -1) {
        // If the file path does not correspond to the fd.
        if (check_filepath_fd(s.fd, fargs->function_path)) {
            int fd = find_fd_filepath(fargs->function_path);
            // Invalid fd, we didn't manage to find a valid fd.
            if (fd < 0) {
                fprintf(stderr, "error, fd %d does not correspond to function path %s\n", s.fd, fargs->function_path);
                return;
            // We found a fd that corresponds to the target file.
            } else {
                fprintf(stderr, "found fd %d to %s\n", fd, fargs->function_path);
                s.fd = fd;
            }
        // else (it corresponds).
        } else {
            s.fd = fargs->function_fd;
        }
    }

    print_mmap(&s);

    // We use the previously returned address and MAP_FIXED to ensure that we recover the correct address.
    ret = (void*) syscall(__NR_mmap, s.ret, s.length, s.prot, s.flags | MAP_FIXED, s.fd, s.offset);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay mmap:\t original ret = %16p got ret = %16p\n",  s.ret, ret);
    }
}

void restore_munmap(struct function_args* fargs) {
    munmap_t s;
    int ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(munmap_t)) != sizeof(munmap_t)) {
        perror("failed to deserialize munmap arguments");
    }

    print_munmap(&s);

    ret = syscall(__NR_munmap, s.addr, s.length);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay munmap:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void restore_mprotect(struct function_args* fargs) {
    mprotect_t s;
    int ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(mprotect_t)) != sizeof(mprotect_t)) {
        perror("failed to deserialize mprotect arguments");
    }

    print_mprotect(&s);

    ret = syscall(__NR_mprotect, s.addr, s.length, s.prot);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay mprotect:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void restore_dup(struct function_args* fargs) {
    dup_t s;
    int ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(dup_t)) != sizeof(dup_t)) {
        perror("failed to deserialize dup arguments");
    }

    print_dup(&s);

    ret = syscall(__NR_dup2, s.oldfd, s.ret);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay dup:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void restore_open(struct function_args* fargs) {
    open_t s;
    int ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(open_t)) != sizeof(open_t)) {
        perror("failed to deserialize open arguments");
    }

    print_open(&s);

    ret = syscall(__NR_open, s.pathname, s.flags, s.mode);
    if (s.ret != ret) {
        // If the file descriptor picked by open is not the expected, try to move it.
        ret = move_fd(ret, s.ret);
        if (s.ret != ret) {
            fprintf(stderr, "failed to replay open:\t original ret = %d got ret = %d\n",  s.ret, ret);
        }
    }
}

void restore_openat(struct function_args* fargs) {
    openat_t s;
    int ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(openat_t)) != sizeof(openat_t)) {
        perror("failed to deserialize openat arguments");
    }

    print_openat(&s);

    ret = syscall(__NR_openat, s.dirfd, s.pathname, s.flags, s.mode);
    if (s.ret != ret) {
        ret = move_fd(ret, s.ret);
        if (s.ret != ret) {
            fprintf(stderr, "failed to replay openat:\t original ret = %d got ret = %d\n",  s.ret, ret);
        }
    }
}

void restore_close(struct function_args* fargs) {
    close_t s;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(close_t)) != sizeof(close_t)) {
        perror("failed to deserialize close arguments");
    }

    print_close(&s);

    // Note: we don't check the value of close because we are not tracking other syscalls that could
    // leave open file descriptors (e.g., socket).
    syscall(__NR_close, s.fd);
}

void checkpoint(struct function_args* fargs, void* isolate) {
    checkpoint_memory(fargs);
    checkpoint_isolate(fargs, fargs->isolate);
}

void* restore(struct function_args* fargs) {
    void* isolate = NULL;

    // Open function library
    fargs->function_fd = open(fargs->function_path, O_RDONLY);
    if (fargs->function_fd < 0) {
        perror("failed to open native image function library file");
    } else {
        // Move to a reserved file descriptor.
        fargs->function_fd = move_fd(fargs->function_fd, 1000);
    }

    // Open the metadata file (syscall arguments, memory ranges, etc).
    fargs->meta_snapshot_fd = open("metadata.snap", O_RDONLY);
    if (fargs->meta_snapshot_fd < 0) {
        perror("failed to open meta snapshot file");
    } else {
        // Move to a reserved file descriptor.
        fargs->meta_snapshot_fd = move_fd(fargs->meta_snapshot_fd, 1001);
    }

    // Open the memory snapshot file.
    int mem_snapshot_fd = open("memory.snap", O_RDONLY);
    if (mem_snapshot_fd < 0) {
        perror("failed to open memory snapshot file");
    } else {
        // Move to a reserved file descriptor.
        mem_snapshot_fd = move_fd(mem_snapshot_fd, 1002);
    }

    while(1) {
        int tag;
        size_t n = read(fargs->meta_snapshot_fd, &tag, sizeof(int));

        if (n == 0) {
            break;
        } else if (n != sizeof(int)) {
            perror("failed to read tag");
        }

        switch (tag)
        {
        case __NR_mmap:
            restore_mmap(fargs);
            break;
        case __NR_munmap:
            restore_munmap(fargs);
            break;
        case __NR_mprotect:
            restore_mprotect(fargs);
            break;
        case __NR_dup:
            restore_dup(fargs);
            break;
        case __NR_open:
            restore_open(fargs);
            break;
        case __NR_openat:
            restore_openat(fargs);
            break;
        case __NR_close:
            restore_close(fargs);
            break;
        case ISOLATE_TAG:
            isolate = restore_isolate(fargs);
            break;
        case MEMORY_TAG:
            restore_memory(fargs, mem_snapshot_fd);
            break;
        default:
            fprintf(stderr, "unknown tag durin restore: %d", tag);
        }
    }

    close(mem_snapshot_fd);
    close(fargs->meta_snapshot_fd);
    return isolate;
}