#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include "../graal_isolate.h"

pthread_t worker;
int finished = 0;

void* run_function(void* args) {
    int myvar = 0;
    while(!finished) {
        printf("myvar = %d\n", myvar++);
        sleep(1);
    }
}

int graal_create_isolate (graal_create_isolate_params_t* params, graal_isolate_t** isolate, graal_isolatethread_t** thread) {
    *thread = NULL;
    *isolate = NULL;
    return 0;
}

int graal_tear_down_isolate(graal_isolatethread_t* thread) {
    finished = 1;
    pthread_join(worker, NULL);
}

void entrypoint(graal_isolatethread_t* thread) {
    pthread_create(&worker, NULL, run_function, NULL);
}

int graal_detach_thread(graal_isolatethread_t* thread) {
    return 0;
}

int graal_attach_thread(graal_isolate_t* isolate, graal_isolatethread_t** thread) {
    *thread = NULL;
    return 0;
}