#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <dlfcn.h>
#include <pthread.h>
#include <sys/syscall.h>
#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <stddef.h>
#include <sys/prctl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <sys/time.h>
#include <errno.h>
#include <poll.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <string.h>

#include "main.h"
#include "list.h"
#include "cr.h"

enum EXECUTION_MODE { NORMAL, CHECKPOINT, RESTORE };

// Wether we are checkpointing or restoreing.
enum EXECUTION_MODE CURRENT_MODE = NORMAL;

int load_isolate_abi(void* dhandle, struct isolate_abi* abi) {
    char* derror = NULL;

    abi->graal_create_isolate = (int (*)(graal_create_isolate_params_t*, graal_isolate_t**, graal_isolatethread_t**)) dlsym(dhandle, "graal_create_isolate");
    if ((derror = dlerror()) != NULL) {
        err("%s\n", derror);
        return 1;
    }

    abi->graal_tear_down_isolate = (int (*)(graal_isolatethread_t*)) dlsym(dhandle, "graal_tear_down_isolate");
    if ((derror = dlerror()) != NULL) {
        err("%s\n", derror);
        return 1;
    }

    abi->entrypoint = (void (*)(graal_isolatethread_t*)) dlsym(dhandle, "entrypoint");
    if ((derror = dlerror()) != NULL) {
        err("%s\n", derror);
        return 1;
    }

    abi->graal_detach_thread = (int (*)(graal_isolatethread_t*)) dlsym(dhandle, "graal_detach_thread");
    if ((derror = dlerror()) != NULL) {
        err("%s\n", derror);
        return 1;
    }

    abi->graal_attach_thread = (int (*)(graal_isolate_t*, graal_isolatethread_t**)) dlsym(dhandle, "graal_attach_thread");
    if ((derror = dlerror()) != NULL) {
        err("%s\n", derror);
        return 1;
    }

    return 0;
}

int install_filter() {
    struct sock_filter filter[] = {
        BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (offsetof(struct seccomp_data, arch))),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, AUDIT_ARCH_X86_64, 1, 0),
        BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_KILL),
        BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (offsetof(struct seccomp_data, nr))),

        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_dup3,    14, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_dup2,    13, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_dup,     11, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_close,   10, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_creat,    9, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_openat2,  8, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_openat,   7, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_open,     6, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_exit,     5, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_clone,    4, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_clone3,   3, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_mprotect, 2, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_mmap,     1, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_munmap,   0, 1),
        BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_USER_NOTIF),

        // default rule
        BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ALLOW),
    };

    struct sock_fprog prog = {
        .len = (unsigned short)(sizeof(filter) / sizeof(filter[0])),
        .filter = filter,
    };

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0)) {
        perror("error: failed to prctl(NO_NEW_PRIVS)");
        return -1;
    }

    int fd = syscall(SYS_seccomp, SECCOMP_SET_MODE_FILTER, SECCOMP_FILTER_FLAG_NEW_LISTENER, &prog);
    if (fd < 0) {
        perror("error: failed to seccomp(SECCOMP_SET_MODE_FILTER)");
        return -1;
    }

    return fd;
}

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

void* run_function(void* args) {
    struct function_args* fargs = (struct function_args*) args;
    graal_isolatethread_t *isolatethread = NULL;

    if (CURRENT_MODE == CHECKPOINT) {
        // Install seccomp filter.
        fargs->seccomp_fd = install_filter();
        if (fargs->seccomp_fd < 0) {
            err("error: failed install seccomp filter\n");
            return NULL;
        }
    }

    // If restoring, attach instead of creating a new isolate.
    if (CURRENT_MODE == RESTORE) {
        fargs->abi.graal_attach_thread(fargs->isolate, &isolatethread);
    } else {
        // Load function library.
        void* dhandle = dlopen(fargs->function_path, RTLD_LAZY);
        if (dhandle == NULL) {
            err("error: failed to load dynamic library: %s\n", dlerror());
            return NULL;
        }

        // Initialize abi.
        if (load_isolate_abi(dhandle, &(fargs->abi))) {
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

void handle_mmap(struct function_args* fargs, void* addr, size_t length, int prot, int flags, int fd, off_t offset, void* ret) {
    mmap_t syscall_args = {.addr = addr, .length = length, .prot = prot, .flags = flags, .fd = fd, .offset = offset, .ret = ret};
    int pagesize = getpagesize();
    print_mmap(&syscall_args);

    // Checkpoint the syscall.
    checkpoint_syscall(fargs, __NR_mmap, &syscall_args, sizeof(mmap_t));

    // When length is not a multiple of pagesize, we extend it to the next page boundary (check mmap man).
    if (length % pagesize) {
        size_t padding = (pagesize - (length % pagesize));
        length = length + padding;
    }

    mapping_t* mapping = list_find(&(fargs->mappings), ret, length);

    // Add mapping if it does not exist yet.
    if (mapping == NULL) {
        mapping = list_push(&(fargs->mappings), ret, length);
    }

    // Update permissions.
    mapping_update_permissions(mapping, ret, ((char*) ret + length), (char) prot);
}

void handle_munmap(struct function_args* fargs, void* addr, size_t length, int ret) {
    munmap_t syscall_args = {.addr = addr, .length = length, .ret = ret};
    int pagesize = getpagesize();
    print_munmap(&syscall_args);

    // Checkpoint the syscall.
    checkpoint_syscall(fargs, __NR_munmap, &syscall_args, sizeof(munmap_t));

     if ((unsigned long)addr % getpagesize()) {
        err("warning, munmap start not a multiple of page boundary\n");
     }

    // When length is not a multiple of pagesize, we extend it to the next page boundary (check mmap man).
    if (length % pagesize) {
        size_t padding = (pagesize - (length % pagesize));
        length = length + padding;
    }

    mapping_t* mapping = list_find(&(fargs->mappings), addr, length);

    // Add mapping if it does not exist yet.
    if (mapping == NULL) {
        err("warning: unmapping unkown mapping %16p - %16p\n", addr, ((char*) addr) + length);
        return;
    }

    // Update mapping size to comply with munmap.
    mapping_update_size(mapping, addr, ((char*) addr) + length);
}

void handle_mprotect(struct function_args* fargs, void* addr, size_t length, int prot, int ret) {
    mprotect_t syscall_args = {.addr = addr, .length = length, .prot = prot, .ret = ret};
    print_mprotect(&syscall_args);

    // Checkpoint the syscall.
    checkpoint_syscall(fargs, __NR_mprotect, &syscall_args, sizeof(mprotect_t));

    mapping_t* mapping = list_find(&(fargs->mappings), addr, length);

    // Add mapping if it does not exist yet.
    if (mapping == NULL) {
        err("warning: mprotecting unkown mapping %16p - %16p\n", addr, ((char*) addr) + length);
        return;
    }

    // Update permissions.
    mapping_update_permissions(mapping, addr, ((char*) addr + length), prot);
}

void handle_dup(struct function_args* fargs, int oldfd, int ret) {
    dup_t syscall_args = {.oldfd = oldfd, .ret = ret};
    print_dup(&syscall_args);
    checkpoint_syscall(fargs, __NR_dup, &syscall_args, sizeof(dup_t));
}

void handle_open(struct function_args* fargs, char* pathname, int flags, mode_t mode, int ret) {
    open_t syscall_args = {.flags = flags, .mode = mode, .ret = ret};

    // If pathname is larger than max, error out, otherwise, copy.
    if (strlen(pathname) > MAX_PATHNAME) {
        err("error, cannot checkpoint pathname longer than %d: %s\n", MAX_PATHNAME, pathname);
    } else {
        strcpy(syscall_args.pathname, pathname);
    }

    print_open(&syscall_args);
    checkpoint_syscall(fargs, __NR_open, &syscall_args, sizeof(open_t));
}

void handle_openat(struct function_args* fargs, int dirfd, char* pathname, int flags, mode_t mode, int ret) {
    openat_t syscall_args = {.dirfd = dirfd, .flags = flags, .mode = mode, .ret = ret};

    // If pathname is larger than max, error out, otherwise, copy.
    if (strlen(pathname) > MAX_PATHNAME) {
        err("error, cannot checkpoint pathname longer than %d: %s\n", MAX_PATHNAME, pathname);
    } else {
        strcpy(syscall_args.pathname, pathname);
    }

    print_openat(&syscall_args);
    checkpoint_syscall(fargs, __NR_openat, &syscall_args, sizeof(openat_t));
}

void handle_close(struct function_args* fargs, int fd, int ret) {
    close_t syscall_args = {.fd = fd, .ret = ret};
    print_close(&syscall_args);
    checkpoint_syscall(fargs, __NR_close, &syscall_args, sizeof(close_t));
}

void handle_notifications(struct function_args* fargs) {
    // This number represents the number of threads that are initially running in the sandbox.
    int active_threads = 1;
    struct seccomp_notif_sizes sizes;
    if (syscall(SYS_seccomp, SECCOMP_GET_NOTIF_SIZES, 0, &sizes) < 0) {
        err("error: failed to seccomp(SECCOMP_GET_NOTIF_SIZES)");
        return;
    }

    struct seccomp_notif *req = (struct seccomp_notif*)malloc(sizes.seccomp_notif);
    struct seccomp_notif_resp *resp = (struct seccomp_notif_resp*)malloc(sizes.seccomp_notif_resp);
    struct pollfd fds[1] = {
        {
            .fd  = fargs->seccomp_fd,
            .events = POLLIN,
        },
    };

    while (active_threads) {

        // Wait for a notification
        int events = poll(fds, 1, 100);
        if (events < 0) {
            err("error: failed to pool for events");
            continue;
        } else if (events == 0 && fargs->finished) {
            err("warning: monitor exiting before all function threads terminate (%d active threads)!\n", active_threads);
            break;
        } else if (fds[0].revents & POLLNVAL) {
            break;
        } else if (events > 1) {
            err("warning: received multiple events at once!\n");
        }

        // Receive notification
        memset(req, 0, sizes.seccomp_notif);
        memset(resp, 0, sizes.seccomp_notif_resp);
        if (ioctl(fargs->seccomp_fd, SECCOMP_IOCTL_NOTIF_RECV, req) == -1) {
            perror("error: failed to ioctl(SECCOMP_IOCTL_NOTIF_RECV)");
            continue;
        }

        // Validate notification
        if (ioctl(fargs->seccomp_fd, SECCOMP_IOCTL_NOTIF_ID_VALID, &req->id) == -1 ) {
            perror("error: failed to ioctl(SECCOMP_IOCTL_NOTIF_ID_VALID)");
            continue;
        }

        // Send response
        resp->id = req->id;
        long long unsigned int *args = req->data.args;
        switch (req->data.nr) {
            // TODO - madvise?
            case __NR_dup:
                resp->val = syscall(__NR_dup, args[0]);
                handle_dup(fargs, (int) args[0], resp->val);
                resp->error = resp->val == -1 ? errno : 0;
                resp->flags = 0;
                break;
            case __NR_open:
                resp->val = syscall(__NR_open, args[0], args[1], args[2]);
                handle_open(fargs, (char*)args[0], (int)args[1], (mode_t)args[2], resp->val);
                resp->error = resp->val == -1 ? errno : 0;
                resp->flags = 0;
                break;
            case __NR_openat:
                resp->val = syscall(__NR_openat, args[0], args[1], args[2], args[3]);
                handle_openat(fargs, (int) args[0], (char*)args[1], (int)args[2], (mode_t)args[3], resp->val);
                resp->error = resp->val == -1 ? errno : 0;
                resp->flags = 0;
                break;
            case __NR_close:
                resp->val = syscall(__NR_close, args[0]);
                handle_close(fargs, (int)args[0], resp->val);
                resp->error = resp->val == -1 ? errno : 0;
                resp->flags = 0;
                break;
            case __NR_exit:
                if (active_threads > 1) {
                    err("warning: exit was invoked!\n");
                }
                active_threads--;
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
            case __NR_clone:
                // TODO - we will need to handle threads. To checkpoint threads, we need to know
                // which threads belong to which sandbox. This is how we could do it:
                // By using this $pid the dumper walks though /proc/$pid/task/ directory collecting
                // threads and through the /proc/$pid/task/$tid/children to gathers children recursively.
                active_threads++;
                err("warning: clone was invoked (%d active threads)!\n", active_threads);
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
            case __NR_clone3:
                active_threads++;
                err("warning: clone3 was invoked (%d active threads)!\n", active_threads);
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
            case __NR_mmap:
                resp->val = syscall(__NR_mmap, args[0], args[1], args[2], args[3], args[4], args[5]);
                handle_mmap(fargs, (void*) args[0], (size_t) args[1], (int) args[2], (int) args[3], (int) args[4], (off_t) args[5], (void*) resp->val);
                resp->error = resp->val >= 0 ? 0 : errno;
                resp->flags = 0;
                break;
            case __NR_munmap:
                resp->val = syscall(__NR_munmap, args[0], args[1]);
                handle_munmap(fargs, (void*) args[0], (size_t) args[1], (int) resp->val);
                resp->error = resp->val >= 0 ? 0 : errno;
                resp->flags = 0;
                break;
            case __NR_mprotect:
                resp->val = syscall(__NR_mprotect, args[0], args[1], args[2]);
                handle_mprotect(fargs, (void*) args[0], (size_t) args[1], (int) args[2], (int) resp->val);
                resp->error = resp->val >= 0 ? 0 : errno;
                resp->flags = 0;
                break;
            default:
                // TODO - in theory, we should be notified on all syscalls.
                err("warning: unhandled syscall %d!\n", req->data.nr);
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
        }
        if (ioctl(fargs->seccomp_fd, SECCOMP_IOCTL_NOTIF_SEND, resp) == -1) {
            perror("error: failed to ioctl(SECCOMP_IOCTL_NOTIF_SEND)");
            continue;
        }
    }

    close(fargs->seccomp_fd);
    free(req);
    free(resp);
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
        restore(&fargs);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("restore took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
#ifdef DEBUG
        print_proc_maps("after_restore.log");
#endif
    }

    // Launch worker thread.
    pthread_create(&thread, NULL, run_function, &fargs);

    // If in checkpoint mode, open metadata file, wait for seccomp to be ready and handle notifications.
    if (CURRENT_MODE == CHECKPOINT) {
        fargs.meta_snapshot_fd = move_to_reserved_fd(open("metadata.snap",  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR));

         // Wait while the thread initilizes and installs the seccomp filter.
        while (!fargs.seccomp_fd) ; // TODO - avoid active waiting

        fargs.seccomp_fd = move_to_reserved_fd(fargs.seccomp_fd);

        // Keep handling syscall notifications.
        handle_notifications(&fargs);
    }

    // Join thread.
    pthread_join(thread, NULL);

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
        checkpoint(&fargs);
#ifdef PERF
        gettimeofday(&et, NULL);
        log("checkpoint took %lu us\n", ((et.tv_sec - st.tv_sec) * 1000000) + (et.tv_usec - st.tv_usec));
#endif
        close(fargs.meta_snapshot_fd);
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
