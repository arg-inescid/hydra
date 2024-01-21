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
#include <string.h>
#include "graal_isolate.h"

// TODO - move this into a demo.
// Note - we assume that there are no other threads attached to an isolate, there are no open files, etc.
// Note - we might need to make sure that all libraries that the isolate depends on are loaded at the same location
// Note - we also need to make sure that the isolate is loaded back to the same location.

// TODO - next steps: set a reserved size for the isolate so that we can quickly find it using mmap.


#define APP "/home/rbruno/git/graalserverless/benchmarks/src/java/gv-hello-world/build/libhelloworld.so"
//#define APP "/home/rbruno/git/graalserverless/benchmarks/src/python/gv-hello-world/build/libhelloworld.so"
//#define APP "/home/rbruno/git/graalserverless/benchmarks/src/javascript/gv-hello-world/build/libhelloworld.so"

// Native Image ABI: https://github.com/oracle/graal/blob/master/substratevm/src/com.oracle.svm.core/headers/graal_isolate.preamble
// Debugging NI binaries: https://www.graalvm.org/22.2/reference-manual/native-image/guides/debug-native-image-process/
// Graal implementation: https://github.com/oracle/graal/blob/a4eada95ef403fdda4c5835fe3299f1dbfdcaecb/substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/c/function/CEntryPointNativeFunctions.java
struct isolate_abi {
    int  (*graal_create_isolate)   (graal_create_isolate_params_t*, graal_isolate_t**, graal_isolatethread_t**);
    int  (*graal_tear_down_isolate)(graal_isolatethread_t*);
    void (*entrypoint)             (graal_isolatethread_t*);
    int  (*graal_detach_thread)    (graal_isolatethread_t*);
    int  (*graal_attach_thread)    (graal_isolate_t*, graal_isolatethread_t**);
};

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

void wait_continue(char* tag) {
    char c;
    printf("[pid = %d, location = %s] Press any key to continue...\n", getpid(), tag);
    scanf("%c",&c);
}
void* run_function(void* arg) {
    struct isolate_abi abi;
    
    wait_continue("Before dlopen");

    void* dhandle = dlopen(APP, RTLD_LAZY);
    graal_isolate_t *isolate = NULL;
    graal_isolatethread_t *thread = NULL;

    if (load_isolate_abi(dhandle, &abi)) {
        fprintf(stderr, "failed to load isolate abi\n");
        return NULL;       
    }

    wait_continue("Before isolate");

    // TODO - decide between loading from disk or creating a new one.
    // TODO - if loading, then attach.
    graal_create_isolate_params_t params;
    memset(&params, 0, sizeof(graal_create_isolate_params_t));
    params.reserved_address_space_size = 1024*1024*512;
    if (abi.graal_create_isolate(&params, &isolate, &thread) != 0) {
        fprintf(stderr, "failed to create isolate\n");
        return NULL;
    }

    wait_continue("Before invoke");
    
    abi.entrypoint(thread);
    
    wait_continue("Before detatch");

    abi.graal_detach_thread(thread);
    
    // TODO - decide if we should write to disk or not.

    wait_continue("Before teardown");
    
    abi.graal_attach_thread(isolate, &thread);
    abi.graal_tear_down_isolate(thread);


    wait_continue("Final");
    return NULL;
}

int install_filter() {
    struct sock_filter filter[] = {
        BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (offsetof(struct seccomp_data, arch))),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, AUDIT_ARCH_X86_64, 1, 0),
        BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_KILL),
        BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (offsetof(struct seccomp_data, nr))),

        // add rules here
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_mmap, 1, 0),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_munmap, 0, 1),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_open, 0, 1),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_openat, 0, 1),
        BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_close, 0, 1),
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

void* run_filtered_function(void* arg) {
    int* fd = (int *)arg;
    
    *fd = install_filter();
    if (*fd < 0) {
        fprintf(stderr, "failed install seccomp filter\n");
        return NULL;
    }
    
    void* ret = run_function(NULL);
    
    close(*fd);
    return ret;
}

void handle_notifications(int fd) {
    struct seccomp_notif_sizes sizes;
    if (syscall(SYS_seccomp, SECCOMP_GET_NOTIF_SIZES, 0, &sizes) < 0) {
        perror("seccomp(SECCOMP_GET_NOTIF_SIZES)");
        exit(1);
    }

    struct seccomp_notif *req = (struct seccomp_notif*)malloc(sizes.seccomp_notif);
    struct seccomp_notif_resp *resp = (struct seccomp_notif_resp*)malloc(sizes.seccomp_notif_resp);
    struct pollfd fds[1] = {
        {
            .fd  = fd,
            .events = POLLIN,
        },
    };

    while (1) {

		// Wait for a notification
        if (poll(fds, 1, -1) <= 0) {
            continue;
        } else if (fds[0].revents & POLLNVAL) {
            break;
        }

        // Receive notification
        memset(req, 0, sizes.seccomp_notif);
        memset(resp, 0, sizes.seccomp_notif_resp);
        if (ioctl(fd, SECCOMP_IOCTL_NOTIF_RECV, req) == -1) {
            perror("ioctl(SECCOMP_IOCTL_NOTIF_RECV)");
            continue;
        }

        // Validate notification
        if (ioctl(fd, SECCOMP_IOCTL_NOTIF_ID_VALID, &req->id) == -1 ) {
            perror("ioctl(SECCOMP_IOCTL_NOTIF_ID_VALID)");
            continue;
        }
    
        // Send response
        resp->id = req->id;
        long long unsigned int *args = req->data.args;
        switch (req->data.nr) {
            case __NR_mmap:
                resp->val = syscall(__NR_mmap, args[0], args[1], args[2], args[3], args[4], args[5]);
                // Ignore when fd != -1, file mapping
                // Ignore when mapping has PROT_NONE
                if (((int)args[4]) == -1 && ((int)args[2] != PROT_NONE)) {
                    fprintf(stderr, "mmap:\t addr = %16p size = %12d prot = %d flags = %8d fd = %2d offset = %8d ret = %16p\n",
                        (void*)args[0], (size_t)args[1], (int)args[2], (int)args[3], (int)args[4], (off_t)args[5], (void*)resp->val);
                }
                resp->error = resp->val >= 0 ? 0 : errno;
                resp->flags = 0;
                break;
            case __NR_munmap:
                fprintf(stderr, "munmap:\t addr = %16p size = %12d\n", (void*)args[0], (size_t)args[1]);
                resp->val = syscall(__NR_munmap, args[0], args[1]);
                resp->error = resp->val >= 0 ? 0 : errno;
                resp->flags = 0;
                break;
            default:
                resp->flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE;
                break;
        }
        if (ioctl(fd, SECCOMP_IOCTL_NOTIF_SEND, resp) == -1) {
            perror("ioctl(SECCOMP_IOCTL_NOTIF_SEND)");
            continue;
        }
    }
    free(req);
    free(resp);
}

int main(int argc, char** argv) {
    int seccomp_fd = 0;
    pthread_t thread;
    pthread_create(&thread, NULL, run_filtered_function, &seccomp_fd);

    while (!seccomp_fd) ; // TODO - avoid active waiting

    handle_notifications(seccomp_fd);

    pthread_join(thread, NULL);
}
