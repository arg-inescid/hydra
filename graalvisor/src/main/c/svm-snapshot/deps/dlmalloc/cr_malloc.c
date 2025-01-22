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
// TODO: use local for optimization
static __thread mspace local = NULL;
static __thread int id = UNINITIALIZED;
static __thread pid_t current_tid = 0;

// mapping of TID to mspace.
static mspace_mapping_t mspace_table[MAX_MSPACE] = {0};
// circular buffer containing notifications to add TID to parent's mspace.
static family_t circular_notif_queue[MAX_NOTIFS];
// counter to denote next position to save/obtain notification.
static int producer_counter = 0;
static int consumer_counter = 0;
// counter to number of existing mspaces
static int mspace_counter = 1;
// helper variable to determine first usage of mspace and creating global mspace
static pid_t base_tid = 0;

mspace_mapping_t* get_mspace_mapping() {
    return mspace_table;
}

// NOT USED ANYMORE
mspace get_mspace() {
    // TODO: add seed as argument to return correct mspace
    return mspace_table[1].mspace;
}

void join_mspace_when_inited(pid_t *future_child, pid_t parent) {
    family_t family = {future_child, parent};
    int position = producer_counter++ % MAX_NOTIFS;
    // if producer wraps around and reaches consumer
    if (position == consumer_counter % MAX_NOTIFS) {
        err(1, "Full notifications queue, increase maximum notification amount");
    }
    circular_notif_queue[position] = family;
}

void inherit_mspace(pid_t child, pid_t parent) {
    // TODO: get_id_from_tid(tid)
    int found_id = 1;
    mspace_mapping_t mapping = {child, mspace_table[found_id].mspace};
    if (mspace_counter == MAX_MSPACE) {
        err(1, "Full mspace table, increase maximum mspace_mapping amount");
    }
    mspace_table[mspace_counter++] = mapping;
    // mspace cur = mspace_table[found_id].mspace;
    // cr_printf(STDOUT_FILENO, "share_mspace with mspace = %p found_id = %d\n", cur, found_id);
}

void consume_notif() {
    int position = consumer_counter++ % MAX_NOTIFS;
    family_t family = circular_notif_queue[position];
    // int my_tid = syscall(__NR_gettid);
    // cr_printf(STDOUT_FILENO, "consume_notif child tid = %d parent tid = %d @ MY_TID = %d\n", *family.child, family.parent, my_tid);
    inherit_mspace(*family.child, family.parent);
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
    mspace_mapping_t mapping = {current_tid, global};
    mspace_table[0] = mapping;
}

void enter_mspace() {
    id = mspace_counter++;
    // if mspace for sandbox doesnt exist
    mspace m = create_mspace(0, 0);
    // TODO: use SEED to check if mspace for sandbox has already been created

    current_tid = syscall(__NR_gettid);
    mspace_mapping_t mapping = {current_tid, m};
    mspace_table[id] = mapping;
}

mspace find_mspace() {

    // if there are notifs to handle
    while (consumer_counter != producer_counter) {
        consume_notif();
    }

    if (!base_tid) {
        base_tid = syscall(__NR_gettid);
    }

    if (!current_tid) {
        current_tid = syscall(__NR_gettid);
    }

    // id not set && main thread
    if (id == UNINITIALIZED && current_tid == base_tid) {
        init_global_mspace();
        id = 0;
        return global;
    }

    // id not set && not main thread
    if (id == UNINITIALIZED) {
        // TODO: get_id_from_tid(tid);
        int result = 2;
        id = result;
        // mspace_mapping_t cur = mspace_table[id];
        // cr_printf(STDOUT_FILENO, "id changed from -1 to %d\n", id);
	    // cr_printf(STDOUT_FILENO, "cur.tid = %d cur.mspace = %p\n", cur.tid, cur.mspace);
        return mspace_table[id].mspace;

    } else if (id == 0) {
        // cr_printf(STDOUT_FILENO, "id=0 and global != NULL\n", id);
	    return global;

    } else if (id != UNINITIALIZED) {
        // mstate cur = mspace_table[id].mspace;
	    // cr_printf(STDOUT_FILENO, "mspace: %p\n", (mspace *) cur);
        return mspace_table[id].mspace;
    }
    return global;
}

void leave_mspace() {
    if (global) {
        id = 0;
    } else {
        id = UNINITIALIZED;
    }
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