#ifndef CR_MALLOC
#define CR_MALLOC

#include <stddef.h>   /* for size_t */
#include "malloc.h"
#include "malloc_internal.h"
#include <sys/types.h>    //NOTE: Added

#define MAX_NOTIFS 10
#define UNINITIALIZED -1

typedef struct mspace_mapping {
    pid_t tid; // TODO - what to use here is not clear.
    mspace mspace;
} mspace_mapping_t;

typedef struct family {
    pid_t *child;
    pid_t parent;
} family_t;

// TODO - add the other memory allocated functions.

void checkpoint_mspace(); // TODO - receive fd, etc?
void restore_mspace(); // TODO - add mspace to the list.

mspace_mapping_t* get_mspace_mapping();
mspace get_mspace();
void enter_mspace();
void join_mspace_when_inited(pid_t*, pid_t);
// void inherit_mspace(pid_t, pid_t);

// dev functions for testing
void inspect_chunks();

// TODO - we need a function to add a particular thread to a new mspace
// TODO - we also need a function to add a particular thread to an existing mspace

#endif
