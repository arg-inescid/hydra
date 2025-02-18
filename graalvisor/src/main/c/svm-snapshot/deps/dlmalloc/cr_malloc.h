#ifndef CR_MALLOC
#define CR_MALLOC

#include "malloc.h"
#include "malloc_internal.h"

#define MAX_MSPACE 1024

// TODO - add the other memory allocated functions.
mspace get_mspace_mapping();

#endif
