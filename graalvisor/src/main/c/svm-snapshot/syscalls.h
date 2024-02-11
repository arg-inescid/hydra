#ifndef SYSCALLS_H
#define SYSCALLS_H

#include <stdio.h>

// Structs used to serialize system call arguments.
typedef struct { void* addr; size_t length; int prot; int flags; int fd; off_t offset; void* ret; } mmap_t;
typedef struct { void* addr; size_t length; int ret; } munmap_t;
typedef struct { void* addr; size_t length; int prot; int ret; } mprotect_t;

// Debug prings for system calls.
void print_mmap(mmap_t* syscall_args);
void print_munmap(munmap_t* syscall_args);
void print_mprotect(mprotect_t* syscall_args);

#endif