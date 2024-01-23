#include <stdio.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "cr.h"
#include "list.h"

#define MEMORY_TAG  -2
#define ISOLATE_TAG -1
#define MMAP_TAG    __NR_mmap
#define MUNMAP_TAG  __NR_munmap

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

// Serialized memory struct.
typedef struct {
    void* addr;
    size_t length;
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

void checkpoint_memory(struct function_args* fargs) {
    int tag = MEMORY_TAG;
    mapping_t* current = &(fargs->mappings);
    int mem_snapshot_fd = open("snapshot.mem",  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR);

    // If the list is not empty
    if (current->start != NULL) {
        while (current != NULL) {
            fprintf(stderr, "checkpoint mapping: start = %16p size = 0x%lx\n", current->start, current->size);
            // Write contents to file.
            memory_to_file(mem_snapshot_fd, current->start, current->size);
            // Write metadata tag.
            if (write(fargs->meta_snapshot_fd, &tag, sizeof(int)) != sizeof(int)) {
                perror("failed to serialize memory tag");
            }
            // Write metadata location.
            memory_t s = {.addr = current->start, .length = current->size};
            if (write(fargs->meta_snapshot_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
                perror("failed to serialize memory header");
            }           
            current = current->next;
        }
    }

    close(mem_snapshot_fd);
}

void restore_memory(struct function_args* fargs, int mem_snapshot_fd) {
    memory_t s;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(memory_t)) != sizeof(memory_t)) {
        perror("failed to deserialize memory header");
    }

    fprintf(stderr, "restonnre mapping: start = %16p size = 0x%lx\n", s.addr, s.length);
    file_to_memory(mem_snapshot_fd, (char*)s.addr, s.length);
}

void checkpoint_isolate(struct function_args* fargs, void* isolate) {
    int tag = ISOLATE_TAG;
    fprintf(stderr, "checkpoint isolate: %16p\n", isolate);
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

void checkpoint_mmap(struct function_args* fargs, void* addr, size_t length, int prot, int flags, int fd, off_t offset, void* ret) {
    if (fargs->meta_snapshot_fd) {
        int tag = MMAP_TAG;
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

    fprintf(stderr, "mmap:\t addr = %16p size = 0x%lx prot = %d flags = %8d fd = %2d offset = %8d ret = %16p\n", 
        s.addr, s.length, s.prot, s.flags, s.fd, s.offset, s.ret);

    // TODO - if we are issuing fd-based, we need to make sure we are loading from the correct file.
    ret = (void*) syscall(__NR_mmap, s.addr, s.length, s.prot, s.flags, s.fd, s.offset);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay mmap:\t original ret = %16p got ret = %16p\n",  s.ret, ret);
    }
}

void checkpoint_munmap(struct function_args* fargs, void* addr, size_t length, int ret) {
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

void restore_munmap(struct function_args* fargs) {
    munmap_t s;
    int ret;

    if (read(fargs->meta_snapshot_fd, &s, sizeof(munmap_t)) != sizeof(munmap_t)) {
        perror("failed to deserialize munmap arguments");
    }

    fprintf(stderr, "munmap:\t addr = %16p size = 0x%lx ret = %d\n", s.addr, s.length, s.ret);

    ret = syscall(__NR_munmap, s.addr, s.length);
    if (s.ret != ret) {
        fprintf(stderr, "failed to replay munmap:\t original ret = %d got ret = %d\n",  s.ret, ret);
    }
}

void* restore(struct function_args* fargs) {
    void* isolate = NULL;
    
    fargs->meta_snapshot_fd = open("snapshot.meta", O_RDONLY);
    if (fargs->meta_snapshot_fd < 0) {
        perror("failed to open meta snapshot file");
    }
    
    int mem_snapshot_fd = open("snapshot.mem", O_RDONLY);
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