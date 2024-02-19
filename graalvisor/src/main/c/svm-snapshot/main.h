#ifndef MAIN_H
#define MAIN_H

#include "graal_isolate.h"
#include "syscalls.h"
#include "list.h"

// Note - we assume that there are no other threads attached to an isolate, there are no open files, etc.
// Note - we might need to make sure that all libraries that the isolate depends on are loaded at the same location
// Note - we also need to make sure that the isolate is loaded back to the same location.

// If defined, enables debug prints and extra sanitization checks.
//#define DEBUG
// If defined, enables performance measurements.
//#define PERF
// If defined, enables performance optimizations.
#define OPT
// Defines the number of function entrypoint invocations per thread.
#define ENTRYPOINT_ITERS 1
// Defines the number of concurrent threads to invoke the function entrypoint.
#define ENTRYPOINT_CONC 1

#define log(format, args...) do { fprintf(stdout, format, ## args); } while(0)
#ifdef DEBUG
    #define dbg(format, args...) do { fprintf(stdout, format, ## args); } while(0)
#else
    #define dbg(format, args...) do { } while(0)
#endif
#define err(format, args...) do { fprintf(stdout, format, ## args); } while(0)

// Number of fds that we allow for the function to use. We may use some fds after this limit.
#define RESERVED_FDS 1020

// Native Image ABI: https://github.com/oracle/graal/blob/master/substratevm/src/com.oracle.svm.core/headers/graal_isolate.preamble
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
    // Pointer to the isolate.
    graal_isolate_t* isolate;
    // File descriptor used when installing seccomp.
    int seccomp_fd;
    // List of memory mappings being tracked in the sandbox.
    mapping_t mappings;
    // Path of the function library.
    char* function_path;
    // Function arguments.
    void* args;
    // Snapshot metadata file descriptor;
    int meta_snapshot_fd;
    // Integer used as a boolean to decide if the function has terminated.
    int finished;
    // TODO - implement file descriptor optimization:
    // keep an array with the syscall call index that openned the file descriptor of the corresponding index.
    int fd_table[RESERVED_FDS];
};

#endif