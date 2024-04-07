#ifndef SVM_SNAPSHOT_H
#define SVM_SHAPSHOT_H

#include "graal_isolate.h"

// Header: https://github.com/oracle/graal/blob/master/substratevm/src/com.oracle.svm.core/headers/graal_isolate.preamble
// C-API: https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/C-API
typedef struct {
    int  (*graal_create_isolate)   (graal_create_isolate_params_t*, graal_isolate_t**, graal_isolatethread_t**);
    int  (*graal_tear_down_isolate)(graal_isolatethread_t*);
    void (*entrypoint)             (graal_isolatethread_t*, const char*, const char*, unsigned long);
    int  (*graal_detach_thread)    (graal_isolatethread_t*);
    int  (*graal_attach_thread)    (graal_isolate_t*, graal_isolatethread_t**);
} isolate_abi_t; // TODO - rename to svm_abi_t

// Loads, runs and then checkpoints a substrate vm instance.
void checkpoint_svm(
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
    const char* fin,
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    char* fout,
    // Length of the output buffer. If the output string is larger, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    unsigned long fout_len,
    // (optional, could be NULL) Pointer to the abi structure where the function
    // pointers will be stored.
    isolate_abi_t* abi,
    // (optional, could be NULL) Output argument used to save the pointer to the
    // restored isolate.
    graal_isolate_t** isolate);

// Loads a checkpointed substrate vm instance.
void restore_svm(
    // Path of the function library that will be dlopened.
    const char* fpath,
    // Path where to store metadata information.
    const char* meta_snap_path,
    // Path where to store memory dumps.
    const char* mem_snap_path,
    // Pointer to the abi structure where the function pointers will be stored.
    isolate_abi_t* abi,
    // Output argument used to save the pointer to the restored isolate.
    graal_isolate_t** isolate);

// Runs the abi entrypoint in an already loaded substrate vm instance.
void run_entrypoint(
    // Pointed to the abi data structure.
    isolate_abi_t* abi,
    // Pointer to the target isolate.
    graal_isolate_t* isolate,
    // Pointer to the target isolate thread.
    graal_isolatethread_t* isolatethread,
    // Number of concurrent threads that will invoke the function code.
    unsigned int concurrency,
    // Number of invocations each thread will perform.
    unsigned int requests,
    // Arguments passed to the function upon each invocation.
    const char* fin,
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    char* fout,
    // Length of the output buffer. If the output string is larger, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    unsigned long fout_len);

// Loads and runs a substrate vm instance.
void run_svm(
    // Path of the function library that will be dlopened.
    const char* fpath,
    // Number of concurrent threads that will invoke the function code.
    unsigned int concurrency,
    // Number of invocations each thread will perform.
    unsigned int requests,
    // Arguments passed to the function upon each invocation.
    const char* fin,
    // Output buffer where the output of the invocation will be placed.
    // Note that if multiple invocations are performed (as a result of concurrency
    // or requests), only the output of the first request will be saved.
    char* fout,
    // Length of the output buffer. If the output string is larger, a warning
    // is printed to stdout and a '\0' is placed at outbuf[outbuf_len - 1].
    unsigned long fout_len,
    // Pointer to the abi structure where the function pointers will be stored.
    isolate_abi_t* abi,
    // Output argument used to save the pointer to the restored isolate.
    graal_isolate_t** isolate);
#endif