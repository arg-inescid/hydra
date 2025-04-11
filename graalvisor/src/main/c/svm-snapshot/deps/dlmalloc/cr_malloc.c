#define _GNU_SOURCE
#include <pthread.h>
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
// Array for storing mutex for each sandbox.
static pthread_mutex_t mutex_table[MAX_MSPACE] = {0};
// Auxiliary variable to know if mutex has already been acquired.
static __thread int in_allocator = 0;

// If the memory allocator becomes the bottleneck it might be worth to re-evaluate thread_locals:
// https://stackoverflow.com/questions/9909980/how-fast-is-thread-local-variable-access-on-linux

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
    if (pthread_mutex_init(&mutex_table[0], NULL) != 0) {
        cr_printf(STDOUT_FILENO, "Mutex initialization failed\n");
        return;
    }
    mspace_count++;
}

mspace find_mspace() {
    if (local) {
        return local;
    }

    if (!current_tid) {
        current_tid = syscall(__NR_gettid);
    }
    // index 0 is used for global mspace
    int mspace_id = (current_tid / 1000);

    // pid = 1 is the system thread, uses global mspace
    if (mspace_id == 0) {
        init_global_mspace();
        mspace_table[mspace_id] = global;
        local = global;
        return local;
    }

    if (!mspace_table[mspace_id]) {
        mspace m = create_mspace(0, 0);
        mspace_table[mspace_id] = m;
        if (pthread_mutex_init(&mutex_table[mspace_id], NULL) != 0) {
            cr_printf(STDOUT_FILENO, "Mutex initialization failed\n");
            return NULL;
        }
        mspace_count++;
    }

    local = mspace_table[mspace_id];
    return local;
}

mspace lock_mspace(int mspace_id) {
    mspace ms = find_mspace();
    if (!in_allocator) {
        pthread_mutex_lock(&mutex_table[mspace_id]);
        in_allocator = 1;
    }
    return ms;
}

void unlock_mspace(int mspace_id) {
    in_allocator = 0;
    pthread_mutex_unlock(&mutex_table[mspace_id]);
}

void* malloc(size_t bytes) {
    dbg("inside malloc\n");
    int mspace_id = (current_tid / 1000);
    int outermost_call = !in_allocator;
    mspace ms = lock_mspace(mspace_id);
    void* ret = mspace_malloc(ms, bytes);
    dbg("malloc(mspace = %p, bytes = %lu) -> %p\n", ms, bytes, ret);
    if (outermost_call) {
        unlock_mspace(mspace_id);
    }
    return ret;
}

void free(void* mem) {
    dbg("inside free\n");
    int mspace_id = (current_tid / 1000);
    int outermost_call = !in_allocator;
    mspace ms = lock_mspace(mspace_id);
    mspace_free(ms, mem);
    dbg("free(mspace = %p, mem = %p)\n", ms, mem);
    if (outermost_call) {
        unlock_mspace(mspace_id);
    }
    return;
}

void* calloc(size_t num, size_t size) {
    dbg("inside calloc\n");
    int mspace_id = (current_tid / 1000);
    int outermost_call = !in_allocator;
    mspace ms = lock_mspace(mspace_id);
    void* ret = mspace_calloc(ms, num, size);
    dbg("calloc(mspace = %p, num = %lu, size = %lu) -> %p\n", ms, num, size, ret);
    if (outermost_call) {
        unlock_mspace(mspace_id);
    }
    return ret;
}

void* realloc(void* ptr, size_t size) {
    dbg("inside realloc\n");
    int mspace_id = (current_tid / 1000);
    int outermost_call = !in_allocator;
    mspace ms = lock_mspace(mspace_id);
    void* ret = mspace_realloc(ms, ptr, size);
    dbg("realloc(mspace = %p, ptr = %p, size = %lu) -> %p\n", ms, ptr, size, ret);
    if (outermost_call) {
        unlock_mspace(mspace_id);
    }
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
