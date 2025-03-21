#include "cr_malloc.h"
#include "../../cr_logger.h"
#include <errno.h>
#include <stdarg.h>
#include <unistd.h>
#include <sys/syscall.h> /* for syscall(__NR_gettid) */
#include <err.h>

// If defined, enables debug prints and extra sanitization checks.
//#define DEBUG

#ifdef DEBUG
    #define dbg(format, args...) do { cr_printf(STDOUT_FILENO, format, ## args); } while(0)
#else
    #define dbg(format, args...) do { } while(0)
#endif

// Global memory pool.
static mspace global = NULL;
// Thread local reference to the memory pool that the thread should use (it can point to global).
static __thread mspace local = NULL;
// Thread local variable that acts as it's TID.
static __thread pid_t current_tid = 0;
// Array for storing mspaces for each sandbox.
static mspace mspace_table[MAX_MSPACE] = {0};
// Number of used mspaces.
static int mspace_count = 0;

mspace get_mspace_mapping() {
    return mspace_table;
}

int get_mspace_count() {
    return mspace_count;
}

void init_global_mspace() {
    mspace newmspace = create_mspace(0, 0);
    mspace uninitialized = NULL;
    if (!__atomic_compare_exchange(&global, &uninitialized, &newmspace, 0, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST)) {
        destroy_mspace(newmspace);
    } else {
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
        mspace_count++;
    }

    local = mspace_table[mspace_id];
    return local;
}


void* malloc(size_t bytes) {
    mspace ms = find_mspace();
    void* ret = mspace_malloc(ms, bytes);
    dbg("malloc(mspace = %p, bytes = %lu) -> %p\n", ms, bytes, ret);
    return ret;
}

void free(void* mem) {
    mspace ms = find_mspace();
    dbg("free(mspace = %p, mem = %p)\n", ms, mem);
    return mspace_free(ms, mem);
}

void* calloc(size_t num, size_t size) {
    mspace ms = find_mspace();
    void* ret = mspace_calloc(ms, num, size);
    dbg("calloc(mspace = %p, num = %lu, size = %lu) -> %p\n", ms, num, size, ret);
    return ret;
}

void* realloc(void* ptr, size_t size) {
    mspace ms = find_mspace();
    void* ret = mspace_realloc(ms, ptr, size);
    dbg("realloc(mspace = %p, ptr = %p, size = %lu) -> %p\n", ms, ptr, size, ret);
    return ret;
}

size_t malloc_usable_size(const void* mem) {
    return dlmalloc_usable_size(mem);
}

struct mallinfo mallinfo() {
    return mspace_mallinfo(find_mspace());
}

int mallopt(int param_number, int value) {
    return dlmallopt(param_number, value);
}

void* memalign(size_t alignment, size_t bytes) {
    return mspace_memalign(find_mspace(), alignment, bytes);
}

void* valloc(size_t size) {
    return dlvalloc(size);
}

void* pvalloc(size_t size) {
    return dlpvalloc(size);
}

void malloc_stats() {
    mspace_malloc_stats(find_mspace());
}

int malloc_info() {
    err(1, "error: malloc_info is not supported)");
    return 0;
}

int malloc_trim(size_t pad) {
    return mspace_trim(find_mspace(), pad);
}

void *reallocarray (void *__ptr, size_t __nmemb, size_t __size) {
    err(1, "error: reallocarray is not supported)");
    return NULL;
}
