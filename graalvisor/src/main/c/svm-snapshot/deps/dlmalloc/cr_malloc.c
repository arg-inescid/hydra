#include "cr_malloc.h"
#include "../../cr_logger.h"
#include "malloc.h"
#include <errno.h>
#include <stdarg.h>
#include <unistd.h>

typedef void* mspace;

typedef struct mspace_mapping {
    pid_t tid; // TODO - what to use here is not clear.
    mspace mspace;

} mspace_mapping_t;

// Global memory pool.
static mspace global = NULL;
// Thread local reference to the memory pool that the thread should use (it can point to global).
static __thread mspace local = NULL;
// This global space
static mspace_mapping_t mspace_table[1024];

void init_global_mspace() {
    mspace newmspace = create_mspace(0, 0);
    mspace uninitialized = NULL;
    if (!__atomic_compare_exchange(&global, &uninitialized, &newmspace, 0, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST)) {
        destroy_mspace(newmspace);
    } else {
        cr_printf(STDOUT_FILENO, "new global mspace -> %p\n", global);
        mspace_track_large_chunks(global, 1);
    }
}

mspace find_mspace() {
    if (local != NULL) {
        return local;
    } // TODO - else if check data structure
    else {
        if (global == NULL) {
            init_global_mspace();
        }
        local = global;
    }
    return local;
}

void* malloc(size_t bytes) {
    void* ret = mspace_malloc(find_mspace(), bytes);
    cr_printf(STDOUT_FILENO, "malloc %d -> %p\n", bytes, ret);
    return ret;
}

void free(void* mem) {
    cr_printf(STDOUT_FILENO, "free %p\n", mem);
    return mspace_free(find_mspace(), mem);
}

void dumper(void* base, size_t size, unsigned int flags) {
    void* limit = ((char*)base) + size;
    cr_printf(STDOUT_FILENO, "segment %p - %p (len %lu) flags %d\n", base, limit, size, flags);

}

void checkpoint_mspace() {
    mspace_inspect_segments(global, dumper);
}

void restore_mspace() {
    // TODO - implement!
}