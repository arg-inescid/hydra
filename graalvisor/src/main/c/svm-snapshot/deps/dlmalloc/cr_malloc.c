#include "cr_malloc.h"
#include "../../cr_logger.h"
#include <errno.h>
#include <stdarg.h>
#include <unistd.h>
#include <sys/syscall.h> /* for syscall(__NR_gettid) */
#include <err.h>

// Global memory pool.
static mspace global = NULL;
// Thread local reference to the memory pool that the thread should use (it can point to global).
static __thread mspace local = NULL;
// Thread local variable that acts as it's TID.
static __thread pid_t current_tid = 0;
// Array for storing mspaces for each sandbox.
static mspace mspace_table[MAX_MSPACE] = {0};

mspace get_mspace_mapping() {
    return mspace_table;
}

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
    if (local) {
        return local;
    }

    if (!current_tid) {
        current_tid = syscall(__NR_gettid);
    }
    // we don't restore global mspace, so seed 0 has pid range 1000-1999 and uses mspace 0.
    int mspace_id = (current_tid / 1000) - 1;
    
    // pid = 1 is the system thread, uses global mspace
    if (mspace_id == -1) {
        init_global_mspace();
        local = global;
        return local;
    }

    if (!mspace_table[mspace_id]) {
        mspace m = create_mspace(0, 0);
        mspace_table[mspace_id] = m;
    }

    local = mspace_table[mspace_id];
    return local;
}


void* malloc(size_t bytes) {
    void* ret = mspace_malloc(find_mspace(), bytes);
    cr_printf(STDOUT_FILENO, "malloc %d -> %p of mspace=%p\n", bytes, ret, find_mspace());
    return ret;
}

void free(void* mem) {
    cr_printf(STDOUT_FILENO, "\t free %p\n", mem);
    return mspace_free(find_mspace(), mem);
}

void* calloc(size_t num, size_t size){
    void* ret = mspace_calloc(find_mspace(), num, size);
    cr_printf(STDOUT_FILENO, "calloc %d elements of %d size -> %p\n", num, size, ret);
    return ret;
}

void* realloc(void* ptr, size_t size){
    void* ret = mspace_realloc(find_mspace(), ptr, size);
    cr_printf(STDOUT_FILENO, "realloc %p to have %d size -> %p\n", ptr, size, ret);
    return ret;
}