#include <stdio.h>
#include <sys/syscall.h>
#include <pthread.h>
#include <unistd.h>
#include "../graal_isolate.h"

pthread_t worker = 0;

void* run_function(void* args) {
    int myvar = 0;
    int tid = syscall(__NR_gettid);
    int pid = getpid();
    while(1) {
        fprintf(stderr, "[background thread] myvar = %d tid = %d pid = %d\n", myvar++, tid, pid);
        sleep(1);
    }
}

int graal_create_isolate (graal_create_isolate_params_t* params, graal_isolate_t** isolate, graal_isolatethread_t** thread) {
    *thread = NULL;
    *isolate = NULL;
    return 0;
}

int graal_tear_down_isolate(graal_isolatethread_t* thread) {
    pthread_join(worker, NULL);
}

void entrypoint(graal_isolatethread_t* thread, const char* fin, const char* fout, unsigned long fout_len) {
    int tid = syscall(__NR_gettid);
    int pid = getpid();
    fprintf(stderr, "[foreground thread] tid = %d pid = %d\n", tid, pid);
    if (worker == 0) {
        pthread_create(&worker, NULL, run_function, NULL);
        sleep(2); // Give the worker the change to print something.
    }
}

int graal_detach_thread(graal_isolatethread_t* thread) {
    return 0;
}

int graal_attach_thread(graal_isolate_t* isolate, graal_isolatethread_t** thread) {
    *thread = NULL;
    return 0;
}
