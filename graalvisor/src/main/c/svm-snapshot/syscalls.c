#include <sys/mman.h>
#include "syscalls.h"

void print_mmap(mmap_t* sargs) {
    fprintf(stderr, "mmap:     %16p - %16p size = 0x%16lx prot = %s%s%s%s flags = %8d fd = %2d offset = %8ld ret = %16p\n",
        sargs->addr,
        sargs->addr == NULL ? NULL : ((char*) sargs->addr) + sargs->length,
        sargs->length,
        sargs->prot & PROT_EXEC   ? "x" : "-",
        sargs->prot & PROT_READ   ? "r" : "-",
        sargs->prot & PROT_WRITE  ? "w" : "-",
        sargs->prot == PROT_NONE  ? "n" : "-",
        sargs->flags,
        sargs->fd,
        sargs->offset,
        sargs->ret);
}

void print_munmap(munmap_t* sargs) {
    fprintf(stderr, "munmap:   %16p - %16p size = 0x%16lx ret  =  %d\n",
        sargs->addr,
        ((char*) sargs->addr) + sargs->length,
        sargs->length,
        sargs->ret);
}

void print_mprotect(mprotect_t* sargs) {
    fprintf(stderr, "mprotect: %16p - %16p size = 0x%16lx prot = %s%s%s%s ret = %d\n",
        sargs->addr,
        ((char*) sargs->addr) + sargs->length,
        sargs->length,
        sargs->prot & PROT_EXEC   ? "x" : "-",
        sargs->prot & PROT_READ   ? "r" : "-",
        sargs->prot & PROT_WRITE  ? "w" : "-",
        sargs->prot == PROT_NONE  ? "n" : "-",
        sargs->ret);
}

void print_dup(dup_t* sargs) {
    fprintf(stderr, "dup(%d) -> %d\n", sargs->oldfd, sargs->ret);
}

void print_open(open_t* sargs) {
    fprintf(stderr, "open(%s) -> %d\n", sargs->pathname, sargs->ret);
}

void print_openat(openat_t* sargs) {
    fprintf(stderr, "openat(%d, %s) -> %d\n", sargs->dirfd, sargs->pathname, sargs->ret);
}

void print_close(close_t* sargs) {
    fprintf(stderr, "close(%d) -> %d\n", sargs->fd, sargs->ret);
}