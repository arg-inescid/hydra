#ifndef CR_MALLOC
#define CR_MALLOC

#include <stddef.h>   /* for size_t */
#include "malloc.h"
#include "malloc_internal.h"
#include <sys/types.h>    //NOTE: Added

#define MAX_MSPACE 1024
#define MAX_NOTIFS 64
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

mspace_mapping_t* get_mspace_mapping();
mspace get_mspace();
void enter_mspace();
void join_mspace_when_inited(pid_t*, pid_t);
void leave_mspace();

#endif
