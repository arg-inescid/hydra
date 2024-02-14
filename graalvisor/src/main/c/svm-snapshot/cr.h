#ifndef CR_H
#define CR_H

#include <sys/types.h>
#include "main.h"

// Prints process memory maps to a give file.
void print_proc_maps(char* filename);

// Checkpoints a single syscall invocation.
void checkpoint_syscall(struct function_args* fargs, int tag, void* syscall_args, size_t size);

// Checkpoints a systrate vm instance.
void checkpoint(struct function_args* fargs);

// Restores a substrace vm instance.
void restore(struct function_args* fargs);

// Checks if a particular file descriptor matches a given path.
int check_filepath_fd(int fd, char* path);
#endif