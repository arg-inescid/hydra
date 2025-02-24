#ifndef CR_MALLOC
#define CR_MALLOC

#include "malloc.h"
#include "malloc_internal.h"
#include <sys/types.h>  /* for pid_t */

#define MAX_MSPACE 1024

mspace get_mspace_mapping();
int get_mspace_count();

#endif
