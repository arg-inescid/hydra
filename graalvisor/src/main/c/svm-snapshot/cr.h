#ifndef CR_H
#define CR_H

#include "svm-snapshot.h"
#include "list.h"
#include <sys/types.h>

/*
 * Limitations:
 * - after an invocation, there should be no open files or sockets, nor running threads, nor sub-processes;
 * - we assume that isolates are independent, i.e., there are no dependencies between them;
 * - we assume that libc (the only external dependency we allow) is always loaded in the same place;
 * - we do not track forked processes (clone without CLONE_VM or CLONE_FILES);
 * - functions should not expect to receive the same tid/pid after restore;
 *
 * Examples of non-supported operations:
 * - leaving a file behind when the function code does not expect that it can be left behind;
 * - mmapping file descriptors passed through unix domain sockets;
 * - forked + exeve is not tracked;
 */

// TODO - we should avoid using glibc as it may interfere with memory maps created by glibc while running the application.
// TODO - implement file descriptor optimization: keep an array with the syscall call index that openned the file descriptor of the corresponding index.

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

// Prints process memory maps to a give file.
void print_proc_maps(char* filename);

// Loads, invokes, and checkpoints an svm instance.
void checkpoint(char* function_path, char* args, char* meta_snap_path, char* mem_snap_path);

// Checkpoints a single syscall invocation.
void checkpoint_syscall(int meta_snap_fd, int tag, void* syscall_args, size_t size);

// Checkpoints an svm instance memory maps.
void checkpoint_memory(int meta_snap_fd, int mem_snap_fd, mapping_t* mappings, isolate_abi_t* abi, graal_isolate_t* isolate);

// Restores a substrace vm instance.
void restore(char* meta_snap_path, char* mem_snap_path, isolate_abi_t* abi, graal_isolate_t** isolate);

// Using dup (or similar syscall), moves oldfd into a reserved fd.
// Note: this function closes the oldfd.
int move_to_reserved_fd(int oldfd);

// Load function (dlopen) and look for abi symbols (dlsym),
int load_function(char* function_path, isolate_abi_t* abi);
#endif