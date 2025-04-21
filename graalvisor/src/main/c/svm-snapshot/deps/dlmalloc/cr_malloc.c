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

// Thread local reference to the memory pool that the thread should use (it can point to global).
static __thread mspace local = NULL;
// Thread local variable that acts as it's TID.
static __thread pid_t current_tid = 0;
// Array for storing mspaces for each sandbox.
static mspace mspace_table[MAX_MSPACE] = {0};
// Number of used mspaces.
static volatile int mspace_count = 0;
// Array for storing mutex for each sandbox.
static pthread_mutex_t mutex_table[MAX_MSPACE] = {0};
// Array for storing mutex attribute for each sandbox.
static pthread_mutexattr_t attr_table[MAX_MSPACE] = {0};

// If the memory allocator becomes the bottleneck it might be worth to re-evaluate thread_locals:
// https://stackoverflow.com/questions/9909980/how-fast-is-thread-local-variable-access-on-linux

mspace get_mspace_mapping() {
    // we skip global mspace because it can't be checkpoint/restored
    return mspace_table[1];
}

int get_mspace_count() {
    return mspace_count;
}

void init_mspace(int mspace_id) {
    mspace newmspace = create_mspace(0, 0);
    mspace uninitialized = NULL;
    if (!__atomic_compare_exchange(&mspace_table[mspace_id], &uninitialized, &newmspace, 0, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST)) {
        destroy_mspace(newmspace);
    } else {
        mspace_track_large_chunks(mspace_table[mspace_id], 1);
        pthread_mutexattr_init(&attr_table[mspace_id]);
        pthread_mutexattr_settype(&attr_table[mspace_id], PTHREAD_MUTEX_RECURSIVE);
        if (pthread_mutex_init(&mutex_table[mspace_id], &attr_table[mspace_id]) != 0) {
            cr_printf(STDOUT_FILENO, "Mutex initialization failed\n");
        }
    }
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

    if (!mspace_table[mspace_id]) {
        init_mspace(mspace_id);
    }

    local = mspace_table[mspace_id];
    return local;
}

mspace lock_mspace(int mspace_id) {
    mspace ms = find_mspace();
    pthread_mutex_lock(&mutex_table[mspace_id]);
    return ms;
}

void unlock_mspace(int mspace_id) {
    pthread_mutex_unlock(&mutex_table[mspace_id]);
}

void* malloc(size_t bytes) {
    dbg("inside malloc\n");
    int mspace_id = (current_tid / 1000);
    mspace ms = lock_mspace(mspace_id);
    void* ret = mspace_malloc(ms, bytes);
    dbg("malloc(mspace = %p, bytes = %lu) -> %p\n", ms, bytes, ret);
    unlock_mspace(mspace_id);
    return ret;
}

void free(void* mem) {
    dbg("inside free\n");
    int mspace_id = (current_tid / 1000);
    mspace ms = lock_mspace(mspace_id);
    mspace_free(ms, mem);
    dbg("free(mspace = %p, mem = %p)\n", ms, mem);
    unlock_mspace(mspace_id);
    return;
}

void* calloc(size_t num, size_t size) {
    dbg("inside calloc\n");
    int mspace_id = (current_tid / 1000);
    mspace ms = lock_mspace(mspace_id);
    void* ret = mspace_calloc(ms, num, size);
    dbg("calloc(mspace = %p, num = %lu, size = %lu) -> %p\n", ms, num, size, ret);
    unlock_mspace(mspace_id);
    return ret;
}

void* realloc(void* ptr, size_t size) {
    dbg("inside realloc\n");
    int mspace_id = (current_tid / 1000);
    mspace ms = lock_mspace(mspace_id);
    void* ret = mspace_realloc(ms, ptr, size);
    dbg("realloc(mspace = %p, ptr = %p, size = %lu) -> %p\n", ms, ptr, size, ret);
    unlock_mspace(mspace_id);
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
