#ifndef CR_MALLOC
#define CR_MALLOC

#include <stddef.h>   /* for size_t */
#include "malloc_internal.h"

// TODO - add the other memory allocated functions.
void* malloc(size_t);
void  free(void*);
void* calloc(size_t num, size_t size);
void* realloc(void* ptr, size_t size);


// Checkpoints the mspace address. It is assumed that all memory was already included in the checkpoint.
void checkpoint_mspace(); // TODO - receive fd, etc?
void restore_mspace(); // TODO - add mspace to the list.

// dev functions for testing
void print_mspace();
void inspect_chunks();

// TODO - we need a function to add a particular thread to a new mspace
// TODO - we also need a function to add a particular thread to an existing mspace

#endif
