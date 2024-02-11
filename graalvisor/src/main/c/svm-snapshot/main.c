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
#include <errno.h>
#include <poll.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <string.h>

#include "main.h"
#include "list.h"
#include "cr.h"

#define DEBUG

enum EXECUTION_MODE { NORMAL, CHECKPOINT, RESTORE };

// Wether we are checkpointing or restoreing.
enum EXECUTION_MODE CURRENT_MODE = NORMAL;

int load_isolate_abi(void* dhandle, struct isolate_abi* abi) {
    char* derror = NULL;

    abi->graal_create_isolate = (int (*)(graal_create_isolate_params_t*, graal_isolate_t**, graal_isolatethread_t**)) dlsym(dhandle, "graal_create_isolate");
    if ((derror = dlerror()) != NULL) {
        fprintf(stderr, "%s\n", derror);
        return 1;
    }

    abi->graal_tear_down_isolate = (int (*)(graal_isolatethread_t*)) dlsym(dhandle, "graal_tear_down_isolate");
    if ((derror = dlerror()) != NULL) {
        fprintf(stderr, "%s\n", derror);
        return 1;
    }

    abi->entrypoint = (void (*)(graal_isolatethread_t*)) dlsym(dhandle, "entrypoint");
    if ((derror = dlerror()) != NULL) {
        fprintf(stderr, "%s\n", derror);
        return 1;
    }

    abi->graal_detach_thread = (int (*)(graal_isolatethread_t*)) dlsym(dhandle, "graal_detach_thread");
    if ((derror = dlerror()) != NULL) {
        fprintf(stderr, "%s\n", derror);
        return 1;
    }

    abi->graal_attach_thread = (int (*)(graal_isolate_t*, graal_isolatethread_t**)) dlsym(dhandle, "graal_attach_thread");
    if ((derror = dlerror()) != NULL) {
        fprintf(stderr, "%s\n", derror);
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

        // TODO - we should probably track open, creat, openat, openat2, and close.
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
        perror("prctl(NO_NEW_PRIVS)");
        return -1;
    }

    int fd = syscall(SYS_seccomp, SECCOMP_SET_MODE_FILTER, SECCOMP_FILTER_FLAG_NEW_LISTENER, &prog);
    if (fd < 0) {
        perror("seccomp(SECCOMP_SET_MODE_FILTER)");
        return -1;
    }

    return fd;
}

void* run_function(void* args) {
    struct function_args* fargs = (struct function_args*) args;
    graal_isolatethread_t *isolatethread = NULL;

    // If checkpointing, install seccomp filter.
    if (CURRENT_MODE == CHECKPOINT) {
        fargs->seccomp_fd = install_filter();
        if (fargs->seccomp_fd < 0) {
            fprintf(stderr, "failed install seccomp filter\n");
            return NULL;
        }
    }

    // If restoring, attach instead of creating a new isolate.
    if (CURRENT_MODE == RESTORE) {
        fargs->abi.graal_attach_thread(fargs->isolate, &isolatethread);
    } else {
        graal_create_isolate_params_t params;
        memset(&params, 0, sizeof(graal_create_isolate_params_t));
        if (fargs->abi.graal_create_isolate(&params, &(fargs->isolate), &isolatethread) != 0) {
            fprintf(stderr, "failed to create isolate\n");
            return NULL;
        }
    }

    // Call function.
    fargs->abi.entrypoint(isolatethread);

    // Detach thread function isolate and quit.
    fargs->abi.graal_detach_thread(isolatethread);
    return NULL;
}

void handle_mmap(struct function_args* fargs, void* addr, size_t length, int prot, int flags, int fd, off_t offset, void* ret) {

    // Checkpoint the syscall.
    checkpoint_mmap(fargs, addr, length, prot, flags, fd, offset, ret);

    mapping_t* mapping = list_find(&(fargs->mappings), ret, length);

    // Add mapping if it does not exist yet.
    if (mapping == NULL) {
        mapping = list_push(&(fargs->mappings), ret, length);
    }

    // Update permissions.
    mapping_update_permissions(mapping, ret, ((char*) ret + length), (char) prot);
}

void handle_munmap(struct function_args* fargs, void* addr, size_t length, int ret) {

    // Checkpoint the syscall.
    checkpoint_munmap(fargs, addr, length, ret);

    mapping_t* mapping = list_find(&(fargs->mappings), addr, length);

    // Add mapping if it does not exist yet.
    if (mapping == NULL) {
        fprintf(stderr, "warning: unmapping unkown mapping %16p - %16p\n", addr, ((char*) addr) + length);
        return;
    }

    // Update mapping size to comply with munmap.
    mapping_update_size(mapping, addr, ((char*) addr) + length);
}

void handle_mprotect(struct function_args* fargs, void* addr, size_t length, int prot, int ret) {

    // Checkpoint the syscall.
    checkpoint_mprotect(fargs, addr, length, prot, ret);

    mapping_t* mapping = list_find(&(fargs->mappings), addr, length);

    // Add mapping if it does not exist yet.
    if (mapping == NULL) {
        fprintf(stderr, "warning: mprotecting unkown mapping %16p - %16p\n", addr, ((char*) addr) + length);
        return;
    }

    // Update permissions.
    mapping_update_permissions(mapping, addr, ((char*) addr + length), prot);
}

void handle_notifications(struct function_args* fargs) {
    // This number represents the number of threads that are initially running in the sandbox.
    int active_threads = 1;
    struct seccomp_notif_sizes sizes;
    if (syscall(SYS_seccomp, SECCOMP_GET_NOTIF_SIZES, 0, &sizes) < 0) {
        perror("seccomp(SECCOMP_GET_NOTIF_SIZES)");
        exit(1);
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
        if (poll(fds, 1, -1) <= 0) {
            continue;
        } else if (fds[0].revents & POLLNVAL) {
            break;
        }

        // Receive notification
        memset(req, 0, sizes.seccomp_notif);
        memset(resp, 0, sizes.seccomp_notif_resp);
        if (ioctl(fargs->seccomp_fd, SECCOMP_IOCTL_NOTIF_RECV, req) == -1) {
            perror("ioctl(SECCOMP_IOCTL_NOTIF_RECV)");
            continue;
        }

        // Validate notification
        if (ioctl(fargs->seccomp_fd, SECCOMP_IOCTL_NOTIF_ID_VALID, &req->id) == -1 ) {
            perror("ioctl(SECCOMP_IOCTL_NOTIF_ID_VALID)");
            continue;
        }

        // Send response
        resp->id = req->id;
        long long unsigned int *args = req->data.args;
        switch (req->data.nr) {
            case __NR_exit:
                if (active_threads > 1) {
                    fprintf(stderr, "warning: exit was invoked!\n");
                }
                active_threads--;
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
            case __NR_clone:
                fprintf(stderr, "warning: clone was invoked!\n");
                active_threads++;
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
            case __NR_clone3:
                fprintf(stderr, "warning: clone3 was invoked!\n");
                active_threads++;
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
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
        }
        if (ioctl(fargs->seccomp_fd, SECCOMP_IOCTL_NOTIF_SEND, resp) == -1) {
            perror("ioctl(SECCOMP_IOCTL_NOTIF_SEND)");
            continue;
        }
    }

    close(fargs->seccomp_fd);
    free(req);
    free(resp);
}

void usage_exit() {
    fprintf(stderr, "Syntax: main <normal|checkpoint|restore> <path to native image app library>\n");
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

    // Zero the entire argument data structure.
    memset(&fargs, 0, sizeof(struct function_args));

    // Initialize based on arguments.
    init_args(&fargs, argc, argv);

    // Load function library.
    void* dhandle = dlopen(fargs.function_path, RTLD_LAZY);
    if (dhandle == NULL) {
        fprintf(stderr, "failed to load dynamic library: %s\n", dlerror());
        return 1;
    }

    // Initialize abi.
    if (load_isolate_abi(dhandle, &(fargs.abi))) {
        fprintf(stderr, "failed to load isolate abi\n");
        return 1;
    }

    // If in restore mode, start by restoring from the snapshot.
    if (CURRENT_MODE == RESTORE) {
        fargs.isolate = restore(&fargs);
#ifdef DEBUG
        print_proc_maps("after_restore.txt"); // TODO - print a tag with the name of the library.
#endif
    }

    // Launch worker thread.
    pthread_create(&thread, NULL, run_function, &fargs);

    // If in checkpoint mode, open metadata file, wait for seccomp to be ready and handle notifications.
    if (CURRENT_MODE == CHECKPOINT) {
        fargs.meta_snapshot_fd = open("metadata.snap",  O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR);
         // Wait while the thread initilizes and installs the seccomp filter.
        while (!fargs.seccomp_fd) ; // TODO - avoid active waiting

        // Keep handling syscall notifications.
        handle_notifications(&fargs);
    }

    // Join thread.
    pthread_join(thread, NULL);

    // If in checkpoint mode, checkpoint memory and isolate address.
   if (CURRENT_MODE == CHECKPOINT) {
#ifdef DEBUG
        print_proc_maps("before_checkpoint.txt");
        print_list(&(fargs.mappings));
#endif
        checkpoint_memory(&fargs);
        checkpoint_isolate(&fargs, fargs.isolate);
        close(fargs.meta_snapshot_fd);
   }

    // Tear down isolate after checkpointing.
    graal_isolatethread_t *isolatethread = NULL;
    fargs.abi.graal_attach_thread(fargs.isolate, &isolatethread);
    fargs.abi.graal_tear_down_isolate(isolatethread);
}
