// Important to load some headers such as sched.h.
#define _GNU_SOURCE

#include "syscalls.h"
#include "list.h"
#include "cr.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/audit.h>
#include <linux/filter.h>
#include <linux/seccomp.h>
#include <linux/sched.h>
#include <poll.h>
#include <pthread.h>
#include <sched.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <unistd.h>

typedef struct {
    // ABI to create, invoke, destroy isolates.
    isolate_abi_t abi;
    // Pointer to the isolate.
    graal_isolate_t* isolate;
    // File descriptor used when installing seccomp.
    int seccomp_fd;
    // Path of the function library.
    char* function_path;
    // Function arguments.
    void* function_args;
    // Integer used as a boolean to decide if the function has terminated.
    int finished;
} checkpoint_worker_args_t;

typedef struct {
     // ABI to create, invoke, destroy isolates.
    isolate_abi_t* abi;
    // Pointer to the isolate.
    graal_isolate_t* isolate;
} entrypoint_worker_args_t;



enum EXECUTION_MODE { NORMAL, CHECKPOINT, RESTORE };

// Wether we are checkpointing or restoreing.
enum EXECUTION_MODE CURRENT_MODE = NORMAL;

void run_serial_entrypoint(isolate_abi_t* abi, graal_isolatethread_t *isolatethread) {
     for (int i = 0; i < ENTRYPOINT_ITERS; i++) {
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        abi->entrypoint(isolatethread);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("entrypoint took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
    }
}

void* entrypoint_worker(void* args) {
    entrypoint_worker_args_t* wargs = (entrypoint_worker_args_t*) args;
    graal_isolatethread_t *isolatethread = NULL;
    wargs->abi->graal_attach_thread(wargs->isolate, &isolatethread);
    run_serial_entrypoint(wargs->abi, isolatethread);
    wargs->abi->graal_detach_thread(isolatethread);
    return NULL;
}

void run_entrypoint(isolate_abi_t* abi, graal_isolate_t* isolate, graal_isolatethread_t* isolatethread) {
    if (ENTRYPOINT_CONC == 1) {
        run_serial_entrypoint(abi, isolatethread);
    } else {
        pthread_t workers[ENTRYPOINT_CONC];
        entrypoint_worker_args_t wargs = { .abi = abi, .isolate = isolate };
        for (int i = 0; i < ENTRYPOINT_CONC; i++) {
            pthread_create(&(workers[i]), NULL, entrypoint_worker, &wargs);
        }
        for (int i = 0; i < ENTRYPOINT_CONC; i++) {
            pthread_join(workers[i], NULL);
        }
    }
}

void run_function(char* function_path, isolate_abi_t* abi, graal_isolate_t** isolate) {
    graal_isolatethread_t *isolatethread = NULL;

    // Load function and initialize abi.
    if (load_function(function_path, abi)) {
        err("error: failed to load isolate abi\n");
        return;
    }

    // Create isolate.
    graal_create_isolate_params_t params;
    memset(&params, 0, sizeof(graal_create_isolate_params_t));
    if (abi->graal_create_isolate(&params, isolate, &isolatethread) != 0) {
        err("error: failed to create isolate\n");
        return;
    }

    // Run user code.
    run_entrypoint(abi, *isolate, isolatethread);

    // Detach thread function isolate and quit.
    abi->graal_detach_thread(isolatethread);
}

void* checkpoint_worker(void* args) {
    checkpoint_worker_args_t* wargs = (checkpoint_worker_args_t*) args;

    if (CURRENT_MODE == CHECKPOINT) {
        // Install seccomp filter.
        wargs->seccomp_fd = install_seccomp_filter();
        if (wargs->seccomp_fd < 0) {
            err("error: failed install seccomp filter\n");
            return NULL;
        }
    }

    // Prepare and run function.
    run_function(wargs->function_path, &(wargs->abi), &(wargs->isolate));

    // Mark execution as finished (will alert the seccomp monitor to quit).
    wargs->finished = 1;
    return NULL;
}

void usage_exit() {
    err("Syntax: main <normal|checkpoint|restore> <path to native image app library>\n");
    exit(1);
}

char* init_args(int argc, char** argv) {
    if (argc != 3) {
        usage_exit();
    } else {
        switch (argv[1][0])
        {
        case 'n':
            CURRENT_MODE = NORMAL;
            break;
        case 'c':
            CURRENT_MODE = CHECKPOINT;
            break;
        case 'r':
            CURRENT_MODE = RESTORE;
            break;
        default:
            usage_exit();
        }
    }
    return argv[2];
}

void run_checkpoint(char* function_path, char* function_args, char* meta_snap_path, char* mem_snap_path) {
    pthread_t worker;

    // Create and initialize the memory mappings list head;
    mapping_t mappings;
    memset(&mappings, 0, sizeof(mapping_t));

     // Create and initialize the checkpoint worker arguments struct.
    checkpoint_worker_args_t wargs;
    memset(&wargs, 0, sizeof(checkpoint_worker_args_t));
    wargs.function_path = function_path;
    wargs.function_args = function_args;

    // If in checkpoint mode, open metadata file, wait for seccomp to be ready and handle notifications.
    int meta_snap_fd = move_to_reserved_fd(open(meta_snap_path,  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR));
    int mem_snap_fd = move_to_reserved_fd(open(mem_snap_path,  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR));
    if (meta_snap_fd < 0 || mem_snap_fd < 0) {
        err("error: failed to open meta (fd = %d) or memory (fd = %d) snapshot file\n", meta_snap_fd, mem_snap_fd);
    } else {
        meta_snap_fd = move_to_reserved_fd(meta_snap_fd);
        mem_snap_fd = move_to_reserved_fd(mem_snap_fd);
    }

    // Launch worker thread.
    pthread_create(&worker, NULL, checkpoint_worker, &wargs); // TODO - move to clone3

    // Wait while the thread initilizes and installs the seccomp filter.
    while (!wargs.seccomp_fd) ; // TODO - avoid active waiting

    // Move seccomp fd to a reserved fd.
    wargs.seccomp_fd = move_to_reserved_fd(wargs.seccomp_fd);

    // Keep handling syscall notifications.
    handle_syscalls(wargs.seccomp_fd, &(wargs.finished), meta_snap_fd, &mappings);

    // Join thread.
    pthread_join(worker, NULL); // TODO - waitpid

    // If in checkpoint mode, checkpoint memory.
#ifdef DEBUG
    print_proc_maps("before_checkpoint.log");
    print_list(&mappings);
#endif
#ifdef PERF
    struct timeval st, et;
    gettimeofday(&st, NULL);
#endif
    checkpoint_memory(meta_snap_fd, mem_snap_fd, &mappings, &(wargs.abi), wargs.isolate);
#ifdef PERF
    gettimeofday(&et, NULL);
    log("checkpoint took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif

    close(meta_snap_fd);
    close(mem_snap_fd);
}

void run_restore(char* meta_snap_path, char* mem_snap_path, isolate_abi_t* abi, graal_isolate_t** isolate) {
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        restore("metadata.snap", "memory.snap", abi, isolate);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("restore took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
#ifdef DEBUG
        print_proc_maps("after_restore.log");
#endif
}

int main(int argc, char** argv) {
    // Check args and get function path.
    char* function_path = init_args(argc, argv);

    // Disable buffering for stdout.
    setvbuf(stdout, NULL, _IONBF, 0);

    // If in restore mode, start by restoring from the snapshot.
    if (CURRENT_MODE == RESTORE) {
        graal_isolate_t* isolate;
        graal_isolatethread_t *isolatethread = NULL;
        isolate_abi_t abi;
        run_restore("metadata.snap", "memory.snap", &abi, &isolate);
        abi.graal_attach_thread(isolate, &isolatethread);
        run_entrypoint(&abi, isolate, isolatethread);
        abi.graal_detach_thread(isolatethread);
    } else if (CURRENT_MODE == CHECKPOINT) {
        run_checkpoint(function_path, NULL, "metadata.snap", "memory.snap");
    } else {
        graal_isolate_t* isolate;
        isolate_abi_t abi;
        run_function(function_path, &abi, &isolate);
    }

    // Flush any open streammed file.
    fflush(NULL);

    // Exit even if there are unfinished threads running in function code.
    exit(0);
}
