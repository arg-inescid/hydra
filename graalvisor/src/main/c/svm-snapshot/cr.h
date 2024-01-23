#ifndef CR_H
#define CR_H

#include <sys/types.h>
#include "main.h"

size_t memory_to_file(int fd, char* buffer, size_t count);
void checkpoint_isolate(struct function_args* fargs, void* isolate);
void checkpoint_memory(struct function_args* fargs);
void checkpoint_mmap(struct function_args* fargs, void* addr, size_t length, int prot, int flags, int fd, off_t offset, void* ret);
void checkpoint_munmap(struct function_args* fargs, void* addr, size_t length, int ret);
void restore_mmap(struct function_args* fargs);
void restore_munmap(struct function_args* fargs);
void* restore(struct function_args* fargs);

#endif