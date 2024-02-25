#ifndef SVM_SNAPSHOT_H
#define SVM_SHAPSHOT_H

#include "graal_isolate.h"

// Native Image ABI: https://github.com/oracle/graal/blob/master/substratevm/src/com.oracle.svm.core/headers/graal_isolate.preamble
typedef struct {
    int  (*graal_create_isolate)   (graal_create_isolate_params_t*, graal_isolate_t**, graal_isolatethread_t**);
    int  (*graal_tear_down_isolate)(graal_isolatethread_t*);
    void (*entrypoint)             (graal_isolatethread_t*);
    int  (*graal_detach_thread)    (graal_isolatethread_t*);
    int  (*graal_attach_thread)    (graal_isolate_t*, graal_isolatethread_t**);
} isolate_abi_t;

// Loads, runs and then checkpoints a substrate vm instance.
void checkpoint_svm(const char* function_path, const char* function_args, unsigned long seed, const char* meta_snap_path, const char* mem_snap_path);

// Loads a checkpointed substrate vm instance.
void restore_svm(const char* meta_snap_path, const char* mem_snap_path, isolate_abi_t* abi, graal_isolate_t** isolate);

// Runs the abi entrypoint in an already loaded substrate vm instance.
void run_entrypoint(isolate_abi_t* abi, graal_isolate_t* isolate, graal_isolatethread_t* isolatethread); // TODO - add args?

// Loads and runs a substrate vm instance.
void run_svm(const char* function_path, isolate_abi_t* abi, graal_isolate_t** isolate); // TODO - args?
#endif