#ifndef CR_MALLOC
#define CR_MALLOC

#include <stddef.h>   /* for size_t */
#include "malloc.h"
#include "malloc_internal.h"
#include <sys/types.h>    //NOTE: Added

#define MAX_MSPACE 1024
#define MAX_NOTIFS 64
#define UNINITIALIZED -1
#define GLOBAL -2

typedef struct mspace_mapping {
    pid_t tid; // TODO - what to use here is not clear.
    mspace mspace;
} mspace_mapping_t;

// Notification with future thread id and corresponding mspace_id
typedef struct notif {
    pid_t *child;
    unsigned int mspace_id;
} notif_t;

// TODO - add the other memory allocated functions.

mspace_mapping_t* get_mspace_mapping();
void recover_mspace(int);
void enter_mspace();
void join_mspace_when_inited(pid_t*, unsigned int);
void leave_mspace();

#endif
