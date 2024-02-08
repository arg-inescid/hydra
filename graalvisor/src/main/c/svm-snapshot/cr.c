#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "cr.h"
#include "list.h"

#define MEMORY_TAG  -2
#define ISOLATE_TAG -1
#define MMAP_TAG    __NR_mmap
#define MUNMAP_TAG  __NR_munmap
#define MPROTECT_TAG  __NR_mprotect

// Serialized mmap arguments struct.
typedef struct {
    void* addr;
    size_t length;
    int prot;
    int flags;
    int fd;
    off_t offset;
    void* ret;
} mmap_t;

// Serialized munmap arguments struct.
typedef struct {
    void* addr;
    size_t length;
    int ret;
} munmap_t;

// Serialized mprotect arguments struct.
typedef struct {
    void* addr;
    size_t length;
    int prot;
    int ret;
} mprotect_t;

// Serialized memory struct.
typedef struct {
    void* addr;
    size_t length;
    int prot;
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

size_t popcount(char* buffer, size_t count) {
    size_t result = 0;
    for (int i = 0; i < count / sizeof(unsigned int); i++) {
        result += __builtin_popcount(*(unsigned int*)buffer);
        buffer += sizeof(unsigned int);
    }
    return result;
}

// TODO - move to main.c
void print_proc_self_maps(char* filename) {
    char buffer[512];
    size_t n = 0;

    int in_fd = open("/proc/self/maps", O_RDONLY);
    if (in_fd < 0) {
        fprintf(stderr, "failed to open proc self maps\n");
        return;
    }

    int out_fd = open(filename,  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR);
    if (out_fd < 0) {
        fprintf(stderr, "failed to open %s\n", filename);
        return;
    }

    while (n = read(in_fd, buffer, sizeof(buffer))) {
        if (n == 0) {
            break;
        } else if (n < 0) {
            if (errno == EINTR || errno == EAGAIN) {
                continue;
            } else {
                fprintf(stderr, "Could not read() data\n");
            }
        } else {
            memory_to_file(out_fd, buffer, n); // TODO - we should also print popcount.
        }
    }
    close(in_fd);
    close(out_fd);
}

// TODO - delete, not used?
void print_library_maps(char* library_path) {
    char buffer[512];
    FILE* file = fopen("/proc/self/maps", "r");
    if (file == NULL) {
        fprintf(stderr, "failed to open proc self maps\n");
        return;
    }

    while (fgets(buffer, sizeof(buffer), file)) {
        if (strstr(buffer, library_path) == NULL) {
            continue;
        } else {
            unsigned long start, finish;
            sscanf(buffer, "%lx-%lx", &start, &finish);

            fprintf(stderr, "start = %16p finish = %16p size = 0x%lx\n",
                (void*)start, (void*) finish, finish - start);
        }
    }
    fclose(file);
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

// A memory block is a set of contiguous memory pages with the same permission.
void checkpoint_memory_block(void* block_start, void* block_finish, char block_perm, int memsnap_fd, int metasnap_fd) {
    int tag = MEMORY_TAG;
    size_t block_size = block_finish - block_start;
    fprintf(stderr, "cmemory:  %16p - %16p size = 0x%16lx prot = %s%s%s%s popcount = %d\n",
        block_start,
        block_finish,
        block_size,
        block_perm & PROT_EXEC   ? "x" : "-",
        block_perm & PROT_READ   ? "r" : "-",
        block_perm & PROT_WRITE  ? "w" : "-",
        block_perm == PROT_NONE  ? "n" : "-",
        popcount(block_start, block_size));

    // Write contents to file.
    memory_to_file(memsnap_fd, block_start, block_size);

    // Write metadata tag.
    if (write(metasnap_fd, &tag, sizeof(int)) != sizeof(int)) {
        perror("failed to serialize memory tag");
    }

    // Write metadata content.
    memory_t s = {.addr = block_start, .length = block_size, .prot = block_perm};
    if (write(metasnap_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
        perror("failed to serialize memory header");
    }
}

void checkpoint_memory_mapping(void* mapping_start, void* mapping_finish, char* mapping_perms, char* mapping_dirty, int memsnap_fd, int metasnap_fd) {
    void* block_start = mapping_start;

    while (block_start < mapping_finish) {
        char  block_dirty = permission(mapping_start, mapping_finish, mapping_dirty, block_start);
        char* block_finish = repeated(mapping_start, mapping_finish, mapping_dirty, block_start);
        if (block_dirty == PROT_WRITE) {
            char  block_perm = permission(mapping_start, mapping_finish, mapping_perms, block_start);
            if (block_finish != repeated(mapping_start, mapping_finish, mapping_perms, block_start)) {
                fprintf(stderr, "error: dirty mapping range overlaps with multiple perm ranges for bock %16p - %16p\n", block_start, block_finish);
            }
            checkpoint_memory_block(block_start, block_finish, block_perm, memsnap_fd, metasnap_fd);
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
    size_t parity = 0;
    int tag = MEMORY_TAG;
    FILE* file = fopen("/proc/self/maps", "r");
    if (file == NULL) {
        fprintf(stderr, "failed to open proc self maps\n");
        return;
    }

    while (fgets(buffer, sizeof(buffer), file)) {
        if (strstr(buffer, fargs->function_path) == NULL) {
            continue;
        } else {
            unsigned long start, finish;
            char r, w, x, p;

            // Reading start and finish addresses
            sscanf(buffer, "%lx-%lx %c%c%c%c", &start, &finish, &r, &w, &x, &p);
            size_t size = finish - start;

            // We ignore mappings of the library that were mmapped by the function.
            if (list_find(&(fargs->mappings), (void*) start, size) != NULL) {
                continue;
            }

            if (w == '-') {
                continue; // TODO - should we skip read/execute-only pages?
            }

            fprintf(stderr, "clibrary: %16p - %16p size = 0x%16lx prot = %c%c%c%c popcount = %d \n",
                start,
                ((char*) start) + size,
                size,
                r, w, x, p,
                popcount((char*)start,
                size));

            // Write contents to file.
            memory_to_file(memsnap_fd, (char*)start, size);

            // Write metadata tag.
            if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
                perror("failed to serialize memory tag");
            }
            // Write metadata content.
            memory_t s = {.addr = (void*)start, .length = size, .prot = 0};
            s.prot = r == 'r' ? s.prot | PROT_READ : s.prot;
            s.prot = w == 'w' ? s.prot | PROT_WRITE : s.prot;
            s.prot = x == 'x' ? s.prot | PROT_EXEC : s.prot;
            // Note: we are not passing the 'p' permission.
            if (write(fargs->meta_snapshot_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
                perror("failed to serialize memory header");
            }

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

    file_to_memory(mem_snapshot_fd, (char*)s.addr, s.length);
    fprintf(stderr, "rmapping  %16p - %16p size = 0x%16lx popcount = %d\n",
        s.addr, ((char*) s.addr) + s.length, s.length, popcount(s.addr, s.length));
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

void print_mmap(void* addr, size_t length, int prot, int flags, int fd, off_t offset, void* ret) {
    fprintf(stderr, "mmap:     %16p - %16p size = 0x%16lx prot = %s%s%s%s flags = %8d fd = %2d offset = %8d ret = %16p\n",
        addr,
        addr == NULL ? NULL : ((char*) addr) + length,
        length,
        prot & PROT_EXEC   ? "x" : "-",
        prot & PROT_READ   ? "r" : "-",
        prot & PROT_WRITE  ? "w" : "-",
        prot == PROT_NONE  ? "n" : "-",
        flags,
        fd,
        offset,
        ret);

}

void print_munmap(void* addr, size_t length, int ret) {
    fprintf(stderr, "munmap:   %16p - %16p size = 0x%16lx ret  =  %d\n",
        addr,
        ((char*) addr) + length,
        length,
        ret);
}

void print_mprotect(void* addr, size_t length, int prot, int ret) {
    fprintf(stderr, "mprotect: %16p - %16p size = 0x%16lx prot = %s%s%s%s ret = %d\n",
        addr,
        ((char*) addr) + length,
        length,
        prot & PROT_EXEC   ? "x" : "-",
        prot & PROT_READ   ? "r" : "-",
        prot & PROT_WRITE  ? "w" : "-",
        prot == PROT_NONE  ? "n" : "-",
        ret);

}

void checkpoint_mmap(struct function_args* fargs, void* addr, size_t length, int prot, int flags, int fd, off_t offset, void* ret) {

    print_mmap(addr, length, prot, flags, fd, offset, ret);

    if (fargs->meta_snapshot_fd) {
        int tag = MMAP_TAG;

        // If we are mapping a file that is not our library.
        if (fd != -1 && check_filepath_fd(fd, fargs->function_path)) {
            fprintf(stderr, "error, fd %d does not correspond to function path %s\n", fd, fargs->function_path);
            return;
        }

        mmap_t s = {.addr = addr, .length = length, .prot = prot, .flags = flags, .fd = fd, .offset = offset, .ret = ret};
        if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
            perror("failed to serialize mmap tag");
        }
        if (write(fargs->meta_snapshot_fd, &s, sizeof(mmap_t)) != sizeof(mmap_t)) {
            perror("failed to serialize mmap arguments");
        }
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

    print_mmap(s.addr, s.length, s.prot, s.flags, s.fd, s.offset, s.ret);

    // We use the previously returned address and MAP_FIXED to ensure that we recover the correct address.
    ret = (void*) syscall(__NR_mmap, s.ret, s.length, s.prot, s.flags | MAP_FIXED, s.fd, s.offset);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay mmap:\t original ret = %16p got ret = %16p\n",  s.ret, ret);
    }
}

void checkpoint_munmap(struct function_args* fargs, void* addr, size_t length, int ret) {

    print_munmap(addr, length, ret);

    if (fargs->meta_snapshot_fd) {
        int tag = MUNMAP_TAG;
        munmap_t s = {.addr = addr, .length = length, .ret = ret};
        if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
            perror("failed to serialize munmap tag");
        }
        if (write(fargs->meta_snapshot_fd, &s, sizeof(munmap_t)) != sizeof(munmap_t)) {
            perror("failed to serialize munmap arguments");
        }
    }
}

void checkpoint_mprotect(struct function_args* fargs, void* addr, size_t length, int prot, int ret) {

    print_mprotect(addr, length, prot, ret);

    if (fargs->meta_snapshot_fd) {
        int tag = MPROTECT_TAG;
        mprotect_t s = {.addr = addr, .length = length, .prot = prot, .ret = ret};
        if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
            perror("failed to serialize mprotect tag");
        }
        if (write(fargs->meta_snapshot_fd, &s, sizeof(mprotect_t)) != sizeof(mprotect_t)) {
            perror("failed to serialize mprotect arguments");
        }
    }
}

void restore_munmap(struct function_args* fargs) {
    munmap_t s;
    int ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(munmap_t)) != sizeof(munmap_t)) {
        perror("failed to deserialize munmap arguments");
    }

    print_munmap(s.addr, s.length, s.ret);

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

    print_mprotect(s.addr, s.length, s.prot, s.ret);

    ret = syscall(__NR_mprotect, s.addr, s.length, s.prot);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay mprotect:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void* restore(struct function_args* fargs) {
    void* isolate = NULL;

    fargs->function_fd = open(fargs->function_path, O_RDONLY);
    if (fargs->function_fd < 0) {
        perror("failed to open native image function library file");
    }

    fargs->meta_snapshot_fd = open("metadata.snap", O_RDONLY);
    if (fargs->meta_snapshot_fd < 0) {
        perror("failed to open meta snapshot file");
    }

    int mem_snapshot_fd = open("memory.snap", O_RDONLY);
    if (mem_snapshot_fd < 0) {
        perror("failed to open memory snapshot file");
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
        case MMAP_TAG:
            restore_mmap(fargs);
            break;
        case MUNMAP_TAG:
            restore_munmap(fargs);
            break;
        case MPROTECT_TAG:
            restore_mprotect(fargs);
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