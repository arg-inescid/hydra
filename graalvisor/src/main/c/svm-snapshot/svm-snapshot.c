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
    // Sandbox for executing entrypoints and getting their results.
    svm_sandbox_t* svm_sandbox;
    // Path to the function library.
    const void* fpath;
    // File descriptor used when installing seccomp.
    int seccomp_fd;
    // Number of parallel threads handling requests.
    int concurrency;
    // Number of requests to perform.
    int requests;
    // Integer used as a boolean to decide if the function has terminated.
    int finished;
    // The seed is used to pass the mspace id.
    unsigned int seed;
} checkpoint_worker_args_t;

typedef struct {
    // Sandbox for executing entrypoints and getting their results.
    svm_sandbox_t* svm_sandbox;
    // Number of parallel threads handling requests.
    int concurrency;
    // Number of requests to perform.
    int requests;
    // Identifies sandbox.
    unsigned long seed;
} notif_worker_args_t;

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
} entrypoint_worker_args_t;

// Dictionary of char* fpath to svm_sandbox_t* svm.
// fpath_to_svm[0] = fpath, fpath_to_svm[1] = svm
void* fpath_to_svm[MAX_SVM * 2] = {0};

void run_serial_entrypoint(isolate_abi_t* abi, graal_isolatethread_t *isolatethread, int requests, const char* fin, char* fout) {
     for (int i = 0; i < requests; i++) {
#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif
        abi->entrypoint(isolatethread, fin, fout, FOUT_LEN);
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
    run_serial_entrypoint(wargs->abi, isolatethread, wargs->requests, wargs->fin, wargs->fout);
    // Note: from manual (https://www.graalvm.org/22.1/reference-manual/native-image/C-API/):
    // 'no code may still be executing in the isolate thread's context.' Since we cannot
    // guarantee that threads may be left behind, it is not safe to detach.
    //wargs->abi->graal_detach_thread(isolatethread);
    return NULL;
}

void run_entrypoint(
            isolate_abi_t* abi,
            graal_isolate_t* isolate,
            graal_isolatethread_t* isolatethread,
            unsigned int concurrency,
            unsigned int requests,
            const char* fin,
            char* fout) {
    if (concurrency == 1) {
        run_serial_entrypoint(abi, isolatethread, requests, fin, fout);
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
        isolate_abi_t* abi,
        graal_isolate_t** isolate) {
    graal_isolatethread_t *isolatethread = NULL;

    // Load function and initialize abi.
    if (load_function(fpath, abi)) {
        err("error: failed to load isolate abi\n");
        return;
    }

    // Create isolate with a no limit (use 256*1024*1024 for 256MB).
    graal_create_isolate_params_t params;
    memset(&params, 0, sizeof(graal_create_isolate_params_t));
    params.version = 1;
    params.reserved_address_space_size = 0;
    if (abi->graal_create_isolate(&params, isolate, &isolatethread) != 0) {
        err("error: failed to create isolate\n");
        return;
    }

    // Run user code.
    run_entrypoint(abi, *isolate, isolatethread, concurrency, requests, fin, fout);

    // Detach thread function isolate and quit. This is safe since all threads are stopped.
    abi->graal_detach_thread(isolatethread);
}

void* checkpoint_worker(void* args) {
    checkpoint_worker_args_t* wargs = (checkpoint_worker_args_t*) args;
    svm_sandbox_t* svm = wargs->svm_sandbox;

    // Install seccomp filter.
    wargs->seccomp_fd = install_seccomp_filter();
    if (wargs->seccomp_fd < 0) {
        err("error: failed install seccomp filter\n");
        return NULL;
    }

    // Prepare and run function.
    run_svm(wargs->fpath, wargs->concurrency, wargs->requests, svm->fin, svm->fout, &svm->abi, &(svm->isolate));
    // Mark execution as finished (will alert the seccomp monitor to quit).
    wargs->finished = 1;

    return NULL;
}

void* notif_worker(void* args) {
    notif_worker_args_t* wargs = (notif_worker_args_t*) args;
    svm_sandbox_t* svm = wargs->svm_sandbox;
    graal_isolatethread_t *isolatethread = NULL;

    (svm->abi).graal_attach_thread(svm->isolate, &isolatethread);
    // Prepare and run function.
    for (;;) {
        pthread_mutex_lock(&svm->mutex);
        // until state is processing or signal to start processing is received, wait.
        while (svm->processing == 0) {
            pthread_cond_wait(&svm->completed_request, &svm->mutex);
        }
        run_entrypoint(&svm->abi, svm->isolate, isolatethread, wargs->concurrency, wargs->requests, svm->fin, svm->fout);
        // set state to finished
        svm->processing = 0;
        pthread_cond_signal(&svm->completed_request);
        pthread_mutex_unlock(&svm->mutex);
    }
    free(wargs);
    return NULL;
}

void invoke_svm(svm_sandbox_t* svm_sandbox, const char* fin, char* fout) {
    pthread_mutex_lock(&svm_sandbox->mutex);
    svm_sandbox->fin = fin;
    svm_sandbox->fout = fout;
    svm_sandbox->processing = 1;
    pthread_cond_signal(&svm_sandbox->completed_request);
    while (svm_sandbox->processing == 1) {
        pthread_cond_wait(&svm_sandbox->completed_request, &svm_sandbox->mutex);
    }
    pthread_mutex_unlock(&svm_sandbox->mutex);
}

svm_sandbox_t* create_sandbox(unsigned long seed) {
    svm_sandbox_t* svm_sandbox = malloc(sizeof(svm_sandbox_t));
    memset(svm_sandbox, 0, sizeof(svm_sandbox_t));
    svm_sandbox->seed = seed;
    svm_sandbox->processing = 0;
    return svm_sandbox;
}

svm_sandbox_t* checkpoint_svm(
        const char* fpath,
        const char* meta_snap_path,
        const char* mem_snap_path,
        size_t seed,
        unsigned int concurrency,
        unsigned int requests,
        const char* fin,
        char* fout) {
    // Thread that will run the sandboxed code.
    pthread_t worker;
    // Thread that will accept all syscalls after checkpoint.
    pthread_t allower;

    // Create and initialize the memory mappings list head;
    mapping_t mappings;
    memset(&mappings, 0, sizeof(mapping_t));

    // Create and initialize the children thread list head;
    thread_t threads;
    memset(&threads, 0, sizeof(thread_t));

    // Create and initialize the checkpoint worker arguments struct.
    checkpoint_worker_args_t* wargs = malloc(sizeof(checkpoint_worker_args_t));
    memset(wargs, 0, sizeof(checkpoint_worker_args_t));

    svm_sandbox_t* svm_sandbox = create_sandbox(seed);
    svm_sandbox->fin = fin;
    svm_sandbox->fout = fout;
    wargs->svm_sandbox = svm_sandbox;
    wargs->fpath = fpath;
    wargs->concurrency = concurrency;
    wargs->requests = requests;
    wargs->seed = seed;

    if (set_next_pid(1000*(seed+1)) == -1) {
        err("set_next_pid");
        return NULL;
    }

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
    pthread_create(&worker, NULL, checkpoint_worker, wargs);

    // Wait while the thread initilizes and installs the seccomp filter.
    while (!wargs->seccomp_fd) ;

    // Move seccomp fd to a reserved fd.
    wargs->seccomp_fd = move_to_reserved_fd(wargs->seccomp_fd);

    // Keep handling syscall notifications.
    handle_syscalls(seed, wargs->seccomp_fd, &(wargs->finished), meta_snap_fd, &mappings, &threads);

    if (!list_threads_empty(&threads)) {
        // Pause background threads before checkpointing.
        pause_background_threads(&threads);
    }

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
    checkpoint(meta_snap_fd, mem_snap_fd, &mappings, &threads, &svm_sandbox->abi, svm_sandbox->isolate);
#ifdef PERF
    gettimeofday(&et, NULL);
    log("checkpoint took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif

    // Join thread after checkpoint (this glibc may free memory right before we checkpoint).
    // Glibc is tricky as it has internal state. We should avoid it at all cost.
    pthread_join(worker, NULL);

    if (!list_threads_empty(&threads)) {
        // Starting monitor thread to allow syscalls from the background threads.
        pthread_create(&allower, NULL, allow_syscalls, &(wargs->seccomp_fd));
        // Resume background threads before checkpointing.
        resume_background_threads(&threads);
    }

    // Launch worker thread and wait for it to finish.
    notif_worker_args_t* restore_wargs = malloc(sizeof(notif_worker_args_t));
    restore_wargs->svm_sandbox = wargs->svm_sandbox;
    restore_wargs->concurrency = wargs->concurrency;
    restore_wargs->requests = wargs->requests;

    pthread_create(&svm_sandbox->thread, NULL, notif_worker, restore_wargs);

    // Close meta and mem fds.
    fsync(meta_snap_fd);
    fsync(mem_snap_fd);
    close(meta_snap_fd);
    close(mem_snap_fd);

    return svm_sandbox;
}

svm_sandbox_t* restore_svm(
        const char* fpath,
        const char* meta_snap_path,
        const char* mem_snap_path,
        size_t seed,
        unsigned int concurrency,
        unsigned int requests,
        const char* fin,
        char* fout) {
    svm_sandbox_t* svm_sandbox = create_sandbox(seed);
    notif_worker_args_t* wargs;
    int last_pid;

#ifdef PERF
        struct timeval st, et;
        gettimeofday(&st, NULL);
#endif

        restore(meta_snap_path, mem_snap_path, &svm_sandbox->abi, &svm_sandbox->isolate);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("restore took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
#ifdef DEBUG
        check_proc_maps("after_restore.log", NULL);
#endif

    wargs = malloc(sizeof(notif_worker_args_t));
    wargs->svm_sandbox = svm_sandbox;
    wargs->concurrency = concurrency;
    wargs->requests = requests;

    // Get pid that will be assigned to next created thread
    if ((last_pid = get_next_pid()) == -1) {
        err("error: failed to get next pid\n");
        return NULL;
    }
    // After having restored threads, set next pid to first pid of current sandbox
    if (set_next_pid(1000*(seed+1)) == -1) {
        err("error: failed to set next pid\n");
        return NULL;
    }

    // Launch worker thread and wait for it to finish.
    pthread_create(&svm_sandbox->thread, NULL, notif_worker, wargs);
    // After launching original application, restore last next_pid for future threads
    if (last_pid != 1) {
        // Threads were restored so we want to change next pid
        if (set_next_pid(last_pid) == -1) {
            err("error: failed to set next pid\n");
            return NULL;
        }
    }
    invoke_svm(svm_sandbox, fin, fout);

    return svm_sandbox;
}

void process_instructions(const char* input_file) {
    char command[10][100] = {0};
    char buffer[100];
    int token_counter = 0;

    FILE *file = fopen(input_file, "r");
    if (file == NULL) {
        err("process_instructions: couldn't open input_file");
        return;
    }

    while (fgets(buffer, sizeof(buffer), file) != NULL) {
        token_counter = 0;
        memset(command, 0, sizeof(command));

        printf("%s", buffer);
        char *token = strtok(buffer, " \t\n");
        while (token != NULL && token_counter < 10) {
            strncpy(command[token_counter], token, 99);
            command[token_counter++][strlen(token)] = '\0';
            token = strtok(NULL, " \t\n");
        }

        for (int i=0; i < token_counter; i++) {
        }

        call_command(token_counter, command);
        printf("\n\n\n");
        memset(buffer, 0, sizeof(buffer));
    }

    fclose(file);
    return;
}

void save_svm(const char* fpath, svm_sandbox_t* svm) {
    size_t index = 0;

    while(fpath_to_svm[index] != NULL) {
        if (index >= MAX_SVM) {
            err("save_svm: surpassed MAX_SVM = %d positions");
            exit(0);
        }
        index += 2;
    }
    char* new_fpath = calloc(strlen(fpath) + 1, 1);
    strcpy(new_fpath, fpath);

    fpath_to_svm[index] = new_fpath;
    fpath_to_svm[index + 1] = svm;

    return;
}

int global_seed = 0;

svm_sandbox_t* get_svm(const char* fpath) {
    svm_sandbox_t* svm = NULL;
    size_t index = 0;

    while(fpath_to_svm[index] != NULL) {
        if (index >= MAX_SVM) {
            err("get_svm: surpassed MAX_SVM = %d positions");
            exit(0);
        }
        printf("compared %s with %s\n", fpath_to_svm[index], fpath);
        if (!strcmp(fpath_to_svm[index], fpath)) {
            svm = fpath_to_svm[index + 1];
            break;
        } else {
            index += 2;
        }
    }

    printf("got svm nr = %ld with svm = %p\n", index, svm);
    global_seed = index / 2;
    return svm;
}


// void init_args(int argc, char** argv) {
void call_command(int argc, char argv[10][100]) {
    const char* FPATH = NULL;
    unsigned int CONC = 1;
    unsigned int ITERS = 1;
    unsigned int SEED = 0;
    const char* fin = "(null)";
    char  fout[FOUT_LEN];

    // isolate_abi_t abi;
    // isolate_abi_t abi = {0};

    graal_isolate_t* isolate = NULL;
    svm_sandbox_t* svm = NULL;

    isolate_abi_t* abi = malloc(sizeof(isolate_abi_t));
    printf("malloc'd abi @ %p\n", abi);

    if (argc < 2) {
        err("wrong utilization!");
        exit(0);
    }

    // Find function code.
    FPATH = argv[1];

    // Find optional arguments.
    if (argc >= 4) {
        CONC = atoi(argv[2]);
    }

    if (argc >= 5) {
        ITERS = atoi(argv[3]);
    }

    if (argc >= 6) {
        SEED = atoi(argv[4]);
    }

    if (argv[0][0] == 'n') {
        run_svm(FPATH, CONC, ITERS, fin, fout, abi, &isolate);
        return;
    }

    svm = get_svm(FPATH);
    printf("global_seed = %d\n", global_seed);
    // if we already have executed this application, reuse its svm
    if (svm != NULL) {
        invoke_svm(svm, fin, fout);
    } else {
        // Find current mode.
        switch (argv[0][0])
        {
        case 'c':
            printf("checkpoint\n");
            svm = checkpoint_svm(FPATH, "metadata.snap", "memory.snap", global_seed, CONC, ITERS, fin, fout);
            save_svm(FPATH, svm);
            break;
        case 'r':
            printf("restore\n");
            svm = restore_svm(FPATH, "metadata.snap", "memory.snap", global_seed, CONC, ITERS, fin, fout);
            // save_svm(FPATH, svm);
            break;
        case 'f':
            err("No file recursion!\n");
            exit(0);
        default:
            err("command not recognized");
        }
    }
    fprintf(stdout, "function(%s) -> %s\n", fin, fout);
}