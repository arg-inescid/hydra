#ifndef SVM_SNAPSHOT_H
#define SVM_SHAPSHOT_H

#include "graal_capi.h"
#include <pthread.h>

// Maximum number of characters to receive from a function invocation.
#define FOUT_LEN 256

// Sandbox for executing entrypoints and getting their results.
typedef struct {
    // Pointer to the abi structure where the function pointers will be stored.
    isolate_abi_t*      abi;
    // Pointer to the isolate where the thread runs.
    graal_isolate_t*    isolate;
    // Pointer to thread running application.
    pthread_t*          thread;
    // Mutex to have exclusive access between worker and request sender.
    pthread_mutex_t*    mutex;
    // Condition variable to signal request status start/finished.
    pthread_cond_t*     completed_request;
    // Predicative variable to avoid deadlocks from signal arriving before
    // other thread started to wait for the signal.
    int                 processing;
    // Arguments passed to the function upon each invocation.
    // Expects null-terminated string with max len FOUT_LEN
    const char*         fin;
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    // If the output string is larger than FOUT_LEN, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    char*               fout;
    // The seed is used to control which virtual memory ranges the svm instance
    // will used. Each seed value represents a 16TB virtual memory range. When
    // calling restore, the user must make sure there is no restoredsvm instance
    // using the same range.
    unsigned long       seed;
} svm_sandbox_t;

// Handle for external use of svm_sandbox on related functions.
typedef svm_sandbox_t* sandbox;

// Executes entrypoint from the provided sandbox.
void invoke_svm(
    // Sandbox for executing entrypoints and getting their results.
    sandbox);

// Loads, runs and then checkpoints a substrate vm instance.
// Returns svm_sandbox_t to be able to invoke more runs.
svm_sandbox_t* checkpoint_svm(
    // Path of the function library that will be dlopened.
    const char* fpath,
    // Path where to store metadata information.
    const char* meta_snap_path,
    // Path where to store memory dumps.
    const char* mem_snap_path,
    // The seed is used to control which virtual memory ranges the svm instance
    // will used. Each seed value represents a 16TB virtual memory range. When
    // calling restore, the user must make sure there is no restoredsvm instance
    // using the same range.
    unsigned long seed,
    // Number of concurrent threads that will invoke the function code.
    unsigned int concurrency,
    // Number of invocations each thread will perform.
    unsigned int requests,
    // Arguments passed to the function upon each invocation.
    // Expects null-terminated string with max len FOUT_LEN
    const char* fin,
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    // If the output string is larger than FOUT_LEN, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    char* fout,
    // (optional, could be NULL) Pointer to the abi structure where the function
    // pointers will be stored.
    isolate_abi_t* abi,
    // (optional, could be NULL) Output argument used to save the pointer to the
    // restored isolate.
    graal_isolate_t** isolate);

// Loads a checkpointed substrate vm instance and then runs it.
// Returns svm_sandbox_t to be able to invoke more runs.
svm_sandbox_t* restore_svm(
    // Path of the function library that will be dlopened.
    const char* fpath,
    // Path where to store metadata information.
    const char* meta_snap_path,
    // Path where to store memory dumps.
    const char* mem_snap_path,
    // The seed is used to control which virtual memory ranges the svm instance
    // will used. Each seed value represents a 16TB virtual memory range. When
    // calling restore, the user must make sure there is no restoredsvm instance
    // using the same range.
    unsigned long seed,
    // Number of concurrent threads that will invoke the function code.
    unsigned int concurrency,
    // Number of invocations each thread will perform.
    unsigned int requests,
    // Arguments passed to the function upon each invocation.
    // Expects null-terminated string with max len FOUT_LEN
    const char* fin,
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    // If the output string is larger than FOUT_LEN, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    char* fout,
    // Arguments passed to the function upon each invocation.
    // Expects null-terminated string with max len FOUT_LEN
    // Pointer to the abi structure where the function pointers will be stored.
    isolate_abi_t* abi,
    // Output argument used to save the pointer to the restored isolate.
    graal_isolate_t** isolate);

// Runs the abi entrypoint in an already loaded substrate vm instance.
void run_entrypoint(
    // Pointer to the abi structure where the function pointers will be stored.
    isolate_abi_t* abi,
    // Output argument used to save the pointer to the restored isolate.
    graal_isolate_t* isolate,
    // Pointer to the target isolate thread.
    graal_isolatethread_t* isolatethread,
    // Number of concurrent threads that will invoke the function code.
    unsigned int concurrency,
    // Number of invocations each thread will perform.
    unsigned int requests,
    // Arguments passed to the function upon each invocation.
    // Expects null-terminated string with max len FOUT_LEN
    const char* fin,
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    // If the output string is larger than FOUT_LEN, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    char* fout);

// Loads and runs a substrate vm instance.
void run_svm(
    // Path of the function library that will be dlopened.
    const char* fpath,
    // Number of concurrent threads that will invoke the function code.
    unsigned int concurrency,
    // Number of invocations each thread will perform.
    unsigned int requests,
    // Arguments passed to the function upon each invocation.
    // Expects null-terminated string with max len FOUT_LEN
    const char* fin,
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    // If the output string is larger than FOUT_LEN, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    char* fout,
    // Pointer to the abi structure where the function pointers will be stored.
    isolate_abi_t* abi,
    // Output argument used to save the pointer to the restored isolate.
    graal_isolate_t** isolate);

#endif
