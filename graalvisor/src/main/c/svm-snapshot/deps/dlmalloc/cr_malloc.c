#include "cr_malloc.h"
#include "../../cr_logger.h"
#include "malloc.h"
#include <errno.h>
#include <stdarg.h>
#include <unistd.h>

//typedef void* mspace;

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
// test cr_malloc inspect_all example
static int count = 0;

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

void dumper(void* base, size_t size, unsigned int flags) {
    void* limit = ((char*)base) + size;
    cr_printf(STDOUT_FILENO, "segment %p - %p (len %lu) flags %d\n", base, limit, size, flags);

}

void count_chunks(void* start, void* end, size_t used, void* arg) {
    if (used >= 100) ++count;
}

void inspect_chunks() {
    // inspects all chunks, allocated and free)
    mspace_inspect_all(global, count_chunks, NULL);
    cr_printf(STDOUT_FILENO, "number of chunks: %d\n", count);
    count = 0;
}

void print_mspace() {
    struct malloc_state* mstate = (struct malloc_state *) global;
    char* least_addr = mstate->least_addr;
    size_t magic = mstate->magic;
    cr_printf(STDOUT_FILENO, "Least addr from malloc_state: %p\n", least_addr);
    cr_printf(STDOUT_FILENO, "Magic from malloc_state: %zu\n", magic);

    size_t dvsize = mstate->dvsize;
    mchunkptr dv = mstate->dv;
    cr_printf(STDOUT_FILENO, "Next chunk will probably be on: %p , unless it's more than %zu bytes.\n", dv, dvsize);
}

void checkpoint_mspace() {
    mspace_inspect_segments(global, dumper);
}

void restore_mspace() {
    // TODO - implement!
}
