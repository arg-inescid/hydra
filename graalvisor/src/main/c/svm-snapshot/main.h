#ifndef MAIN_H
#define MAIN_H

#include "graal_isolate.h"
#include "list.h"

// TODO - move this into a demo.
// Note - we assume that there are no other threads attached to an isolate, there are no open files, etc.
// Note - we might need to make sure that all libraries that the isolate depends on are loaded at the same location
// Note - we also need to make sure that the isolate is loaded back to the same location.

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

struct function_args {
    // ABI to create, invoke, destroy isolates.
    struct isolate_abi abi;
    // File descriptor used when installing seccomp.
    int seccomp_fd;
    // List of memory mappings being tracked in the sandbox.
    mapping_t mappings;
    // Function arguments.
    void* args;
    // Snapshot metadata file descriptor;
    int meta_snapshot_fd;
};

#endif