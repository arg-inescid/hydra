#include "cr.h"
#include "list_threads.h"
#include "syscalls.h"

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
    // Path to the function library.
    const void* fpath;
    // ABI to create, invoke, destroy isolates.
    isolate_abi_t abi;
    // Pointer to the isolate.
    graal_isolate_t* isolate;
    // File descriptor used when installing seccomp.
    int seccomp_fd;
    // Path of the function library.
    const char* fin;
    // Return string from function invocation.
    char* fout;
    // Length of the output buffer.
    size_t fout_len;
    // Number of parallel threads handling requests.
    int concurrency;
    // Number of requests to perform.
    int requests;
    // Integer used as a boolean to decide if the function has terminated.
    int finished;
} checkpoint_worker_args_t;

typedef struct {
     // ABI to create, invoke, destroy isolates.
    isolate_abi_t* abi;
    // Pointer to the isolate.
    graal_isolate_t* isolate;
    // Number of requests to perform.
    int requests;
    // Function arguments.
    const char* fin;
    // Return string from function invocation.
    char* fout;
    // Length of the output buffer.
    size_t fout_len;
} entrypoint_worker_args_t;

void run_serial_entrypoint(isolate_abi_t* abi, graal_isolatethread_t *isolatethread, int requests, const char* fin, char* fout, size_t fout_len) {
     for (int i = 0; i < requests; i++) {
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        abi->entrypoint(isolatethread, fin, fout, fout_len);
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
    run_serial_entrypoint(wargs->abi, isolatethread, wargs->requests, wargs->fin, wargs->fout, wargs->fout_len);
    wargs->abi->graal_detach_thread(isolatethread);
    return NULL;
}

void run_entrypoint(
            isolate_abi_t* abi,
            graal_isolate_t* isolate,
            graal_isolatethread_t* isolatethread,
            unsigned int concurrency,
            unsigned int requests,
            const char* fin,
            char* fout,
            size_t fout_len) {
    if (concurrency == 1) {
        run_serial_entrypoint(abi, isolatethread, requests, fin, fout, fout_len);
    } else {
        pthread_t* workers = (pthread_t*) malloc(concurrency * sizeof(pthread_t));
        entrypoint_worker_args_t* wargs = (entrypoint_worker_args_t*) malloc(concurrency * sizeof(entrypoint_worker_args_t));
        for (int i = 0; i < concurrency; i++) {
            wargs[i].abi = abi;
            wargs[i].isolate = isolate;
            wargs[i].requests = requests;
            wargs[i].fin = fin;
            // When multiple threads are launched, the output is taken from the first.
            wargs[i].fout = i == 0 ? fout : NULL;
            wargs[i].fout_len = i == 0 ? fout_len : 0;
            pthread_create(&(workers[i]), NULL, entrypoint_worker, &(wargs[i]));
        }
        for (int i = 0; i < concurrency; i++) {
            pthread_join(workers[i], NULL);
        }
        free(workers);
        free(wargs);
    }
}

void run_svm(
        const char* fpath,
        unsigned int concurrency,
        unsigned int requests,
        const char* fin,
        char* fout,
        size_t fout_len,
        isolate_abi_t* abi,
        graal_isolate_t** isolate) {
    graal_isolatethread_t *isolatethread = NULL;

    // Load function and initialize abi.
    if (load_function(fpath, abi)) {
        err("error: failed to load isolate abi\n");
        return;
    }

    // Create isolate.
    graal_create_isolate_params_t params;
    // TODO - set to 256MB!
    memset(&params, 0, sizeof(graal_create_isolate_params_t));
    if (abi->graal_create_isolate(&params, isolate, &isolatethread) != 0) {
        err("error: failed to create isolate\n");
        return;
    }

    // Run user code.
    run_entrypoint(abi, *isolate, isolatethread, concurrency, requests, fin, fout, fout_len);

    // Detach thread function isolate and quit.
    abi->graal_detach_thread(isolatethread);
}

void* checkpoint_worker(void* args) {
    checkpoint_worker_args_t* wargs = (checkpoint_worker_args_t*) args;

    // Install seccomp filter.
    wargs->seccomp_fd = install_seccomp_filter();
    if (wargs->seccomp_fd < 0) {
        err("error: failed install seccomp filter\n");
        return NULL;
    }

    // Prepare and run function.
    run_svm(wargs->fpath, wargs->concurrency, wargs->requests, wargs->fin, wargs->fout, wargs->fout_len, &(wargs->abi), &(wargs->isolate));

    // Mark execution as finished (will alert the seccomp monitor to quit).
    wargs->finished = 1;
    return NULL;
}

void checkpoint_svm(
        const char* fpath,
        const char* meta_snap_path,
        const char* mem_snap_path,
        size_t seed,
        unsigned int concurrency,
        unsigned int requests,
        const char* fin,
        char* fout,
        size_t fout_len,
        isolate_abi_t* abi,
        graal_isolate_t** isolate) {
    pthread_t worker;

    // Create and initialize the memory mappings list head;
    mapping_t mappings;
    memset(&mappings, 0, sizeof(mapping_t));

    // Create and initialize the children thread list head;
    thread_t threads;
    memset(&threads, 0, sizeof(thread_t));

     // Create and initialize the checkpoint worker arguments struct.
    checkpoint_worker_args_t wargs;
    memset(&wargs, 0, sizeof(checkpoint_worker_args_t));
     wargs.fpath = fpath;
     wargs.fin = fin;
     wargs.fout = fout;
     wargs.fout_len = fout_len;
     wargs.concurrency = concurrency;
     wargs.requests = requests;

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
    pthread_create(&worker, NULL, checkpoint_worker, &wargs);

    // Wait while the thread initilizes and installs the seccomp filter.
    while (!wargs.seccomp_fd) ;

    // Move seccomp fd to a reserved fd.
    wargs.seccomp_fd = move_to_reserved_fd(wargs.seccomp_fd);

    // Keep handling syscall notifications.
    handle_syscalls(seed, wargs.seccomp_fd, &(wargs.finished), meta_snap_fd, &mappings, &threads);

    // Pause background threads before checkpointing.
    pause_background_threads(&threads);

    // Merge memory mappings.
    list_mappings_merge(&mappings);

    // If in checkpoint mode, checkpoint memory.
    check_proc_maps("before_checkpoint.log", &mappings);
#ifdef DEBUG
    print_list_mappings(&mappings);
#endif
#ifdef PERF
    struct timeval st, et;
    gettimeofday(&st, NULL);
#endif
    checkpoint(meta_snap_fd, mem_snap_fd, &mappings, &threads, &(wargs.abi), wargs.isolate);
#ifdef PERF
    gettimeofday(&et, NULL);
    log("checkpoint took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif

    // Resume background threads before checkpointing.
    resume_background_threads(&threads);

    // Join thread after checkpoint (this glibc may free memory right before we checkpoint).
    // Glibc is tricky as it has internal state. We should avoid it at all cost.
    pthread_join(worker, NULL);

    // Close meta and mem fds.
    close(meta_snap_fd);
    close(mem_snap_fd);

    // Copy output arguments.
    if (abi != NULL) {
        memcpy(abi, &(wargs.abi), sizeof(isolate_abi_t));
    }
    if (isolate != NULL) {
        memcpy(isolate, &(wargs.isolate), sizeof(graal_isolate_t*));
    }
}

void restore_svm(const char* fpath, const char* meta_snap_path, const char* mem_snap_path, isolate_abi_t* abi, graal_isolate_t** isolate) {
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        restore(meta_snap_path, mem_snap_path, abi, isolate);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("restore took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
#ifdef DEBUG
        check_proc_maps("after_restore.log", NULL);
#endif
}