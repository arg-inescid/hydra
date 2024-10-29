#include <stdio.h>
#include <stdlib.h>
#include "../graal_isolate.h"

void* buff = NULL;

int graal_create_isolate (graal_create_isolate_params_t* params, graal_isolate_t** isolate, graal_isolatethread_t** thread) {
    *thread = NULL;
    *isolate = NULL;
    return 0;
}

int graal_tear_down_isolate(graal_isolatethread_t* thread) {
    return 0;
}

void entrypoint(graal_isolatethread_t* thread, const char* fin, const char* fout, unsigned long fout_len) {

    if (buff != NULL) {
        printf("freeing buff = %p\n", buff);
        free(buff);
    }

    void* garbage = malloc(1024 * sizeof(unsigned long long int));
    buff = malloc(1024 * sizeof(char));
    printf("malloced buff = %p\n", buff);
    free(garbage);
}

int graal_detach_thread(graal_isolatethread_t* thread) {
    return 0;
}


int graal_attach_thread(graal_isolate_t* isolate, graal_isolatethread_t** thread) {
    *thread = NULL;
    return 0;
}
