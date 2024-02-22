#ifndef SYSCALLS_H
#define SYSCALLS_H

#include <stdio.h>
#include <fcntl.h>

#define MAX_PATHNAME 255

// Structs used to serialize system call arguments.
typedef struct { void* addr; size_t length; int prot; int flags; int fd; off_t offset; void* ret; } mmap_t;
typedef struct { void* addr; size_t length; int ret; } munmap_t;
typedef struct { void* addr; size_t length; int prot; int ret; } mprotect_t;
typedef struct { void* addr; size_t length; int advice; int ret; } madvise_t;
typedef struct { int oldfd; int ret; } dup_t;
typedef struct { char pathname[MAX_PATHNAME]; int flags; mode_t mode; int ret; } open_t;
typedef struct { int dirfd; char pathname[MAX_PATHNAME]; int flags; mode_t mode; int ret; } openat_t;
typedef struct { int fd; int ret; } close_t;

// Debug prings for system calls.
void print_mmap(mmap_t* syscall_args);
void print_munmap(munmap_t* syscall_args);
void print_mprotect(mprotect_t* syscall_args);
void print_madvise(madvise_t* syscall_args);
void print_dup(dup_t* syscall_args);
void print_open(open_t* syscall_args);
void print_openat(openat_t* syscall_args);
void print_close(close_t* syscall_args);

#endif