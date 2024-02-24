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

struct function_args {
    // ABI to create, invoke, destroy isolates.
    struct isolate_abi abi;
    // Pointer to the isolate.
    graal_isolate_t* isolate;
    // File descriptor used when installing seccomp.
    int seccomp_fd;
    // List of memory mappings being tracked in the sandbox.
    mapping_t mappings;
    // Path of the function library.
    char* function_path;
    // Function arguments.
    void* args;
    // Snapshot metadata file descriptor;
    int meta_snap_fd;
    // Snapshot memory file descriptor;
    int mem_snap_fd;
    // Integer used as a boolean to decide if the function has terminated.
    int finished;
};

enum EXECUTION_MODE { NORMAL, CHECKPOINT, RESTORE };

// Wether we are checkpointing or restoreing.
enum EXECUTION_MODE CURRENT_MODE = NORMAL;

void run_serial_entrypoint(struct function_args* fargs, graal_isolatethread_t *isolatethread) {
     for (int i = 0; i < ENTRYPOINT_ITERS; i++) {
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        fargs->abi.entrypoint(isolatethread);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("entrypoint took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
    }
}

void* run_parallel_entrypoint(void* args) {
    struct function_args* fargs = (struct function_args*) args;
    graal_isolatethread_t *isolatethread = NULL;
    fargs->abi.graal_attach_thread(fargs->isolate, &isolatethread);
    run_serial_entrypoint(fargs, isolatethread);
    fargs->abi.graal_detach_thread(isolatethread);
    return NULL;
}

// TODO - we need to untangle this in two functions in cr.c.
void* run_function(void* args) {
    struct function_args* fargs = (struct function_args*) args;
    graal_isolatethread_t *isolatethread = NULL;

    if (CURRENT_MODE == CHECKPOINT) {
        // Install seccomp filter.
        fargs->seccomp_fd = install_seccomp_filter();
        if (fargs->seccomp_fd < 0) {
            err("error: failed install seccomp filter\n");
            return NULL;
        }
    }

    // If restoring, attach instead of creating a new isolate.
    if (CURRENT_MODE == RESTORE) {
        fargs->abi.graal_attach_thread(fargs->isolate, &isolatethread);
    } else {
        // Initialize abi.
        if (load_function(fargs->function_path, &(fargs->abi))) {
            err("error: failed to load isolate abi\n");
            return NULL;
        }

        // Create isolate.
        graal_create_isolate_params_t params;
        memset(&params, 0, sizeof(graal_create_isolate_params_t));
        if (fargs->abi.graal_create_isolate(&params, &(fargs->isolate), &isolatethread) != 0) {
            err("error: failed to create isolate\n");
            return NULL;
        }
    }

    // Call function.
    if (ENTRYPOINT_CONC == 1) {
        run_serial_entrypoint(fargs, isolatethread);
    } else {
        pthread_t workers[ENTRYPOINT_CONC];
        for (int i = 0; i < ENTRYPOINT_CONC; i++) {
            pthread_create(&(workers[i]), NULL, run_parallel_entrypoint, fargs);
        }
        for (int i = 0; i < ENTRYPOINT_CONC; i++) {
            pthread_join(workers[i], NULL);
        }
    }

    // Detach thread function isolate and quit.
    fargs->abi.graal_detach_thread(isolatethread);
    fargs->finished = 1;
    return NULL;
}

void usage_exit() {
    err("Syntax: main <normal|checkpoint|restore> <path to native image app library>\n");
    exit(1);
}

void init_args(struct function_args* fargs, int argc, char** argv) {
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
        fargs->function_path = argv[2];
    }
}

int main(int argc, char** argv) {
    struct function_args fargs;
    pthread_t thread;

    // Disable buffering for stdout.
    setvbuf(stdout, NULL, _IONBF, 0);

    // Zero the entire argument data structure.
    memset(&fargs, 0, sizeof(struct function_args));

    // Initialize based on arguments.
    init_args(&fargs, argc, argv);

    // If in restore mode, start by restoring from the snapshot.
    if (CURRENT_MODE == RESTORE) {
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        restore("metadata.snap", "memory.snap", &(fargs.abi), &(fargs.isolate));
#ifdef PERF
        gettimeofday(&et, NULL);
        log("restore took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
#ifdef DEBUG
        print_proc_maps("after_restore.log");
#endif
    }

    // Launch worker thread.
    pthread_create(&thread, NULL, run_function, &fargs); // TODO - move to clone3

    // If in checkpoint mode, open metadata file, wait for seccomp to be ready and handle notifications.
    if (CURRENT_MODE == CHECKPOINT) {
        int meta_snap_fd = move_to_reserved_fd(open("metadata.snap",  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR));
        int mem_snap_fd = move_to_reserved_fd(open("memory.snap",  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR));
        if (meta_snap_fd < 0 || mem_snap_fd < 0) {
            err("error: failed to open meta (fd = %d) or memory (fd = %d) snapshot file\n", meta_snap_fd, mem_snap_fd);
        } else {
            fargs.meta_snap_fd = move_to_reserved_fd(meta_snap_fd);
            fargs.mem_snap_fd = move_to_reserved_fd(mem_snap_fd);
        }

         // Wait while the thread initilizes and installs the seccomp filter.
        while (!fargs.seccomp_fd) ; // TODO - avoid active waiting

        fargs.seccomp_fd = move_to_reserved_fd(fargs.seccomp_fd);

        // Keep handling syscall notifications.
        handle_syscalls(fargs.seccomp_fd, &(fargs.finished), fargs.meta_snap_fd, &(fargs.mappings));
    }

    // Join thread.
    pthread_join(thread, NULL); // TODO - waitpid

    // If in checkpoint mode, checkpoint memory and isolate address.
   if (CURRENT_MODE == CHECKPOINT) {
#ifdef DEBUG
        print_proc_maps("before_checkpoint.log");
        print_list(&(fargs.mappings));
#endif
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        checkpoint_memory(fargs.meta_snap_fd, fargs.mem_snap_fd, &(fargs.mappings), &(fargs.abi), fargs.isolate);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("checkpoint took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
        close(fargs.meta_snap_fd);
        close(fargs.mem_snap_fd);
   }

    // Flush any open streammed file.
    fflush(NULL);

    // Exit even if there are unfinished threads running in function code.
    exit(0);

    // Tear down isolate after checkpointing.
    graal_isolatethread_t *isolatethread = NULL;
    fargs.abi.graal_attach_thread(fargs.isolate, &isolatethread);
    fargs.abi.graal_tear_down_isolate(isolatethread);
}
