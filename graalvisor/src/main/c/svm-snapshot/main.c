// Important to load some headers such as sched.h.
#define _GNU_SOURCE

#include "svm-snapshot.h"
#include <stdio.h>
#include <stdlib.h>

// Maximum number of characters to receive from a function invocation.
#define FOUT_LEN 256

enum EXECUTION_MODE { NORMAL, CHECKPOINT, RESTORE };

// Wether we are checkpointing or restoreing.
enum EXECUTION_MODE CURRENT_MODE = NORMAL;

// Path to function library.
const char* FPATH = NULL;

// Defines the number of function entrypoint invocations per thread.
unsigned int ITERS  = 1;

// Defines the number of concurrent threads to invoke the function entrypoint.
unsigned int CONC = 1;

// The seed is used to control which virtual memory ranges the svm instance will used (see svm-snapshot.h).
unsigned int SEED = 0;

void usage_exit() {
    fprintf(stderr, "Syntax: main <normal|checkpoint|restore> <path to native image app library> [concurrency [iterations [seed]]]\n");
    fprintf(stderr, "Optional: concurrency (defaults to 1).\n");
    fprintf(stderr, "Optional: iterations (defaults to 1).\n");
    fprintf(stderr, "Optional: seed (defaults to zero and ignored in normal and restore modes).\n");
    exit(1);
}

void init_args(int argc, char** argv) {
    if (argc < 3) {
        usage_exit();
    }

    // Find current mode.
    switch (argv[1][0])
    {
    case 'n':
        CURRENT_MODE = NORMAL;
        break;
    case 'c':
        CURRENT_MODE = CHECKPOINT;
        break;
    case 'r':
        CURRENT_MODE = RESTORE;
        break;
    default:
        usage_exit();
    }

    // Find function code.
    FPATH = argv[2];

    // Find optional arguments.
    if (argc >= 4) {
        CONC = atoi(argv[3]);
    }

    if (argc >= 5) {
        ITERS = atoi(argv[4]);
    }

    if (argc >= 6) {
        SEED = atoi(argv[5]);
    }
}

int main(int argc, char** argv) {
    const char* fin = "(null)";
    char  fout[FOUT_LEN];

    // Initialize arguments.
    init_args(argc, argv);

    // Disable buffering for stdout.
    setvbuf(stdout, NULL, _IONBF, 0);

    // If in restore mode, start by restoring from the snapshot.
    if (CURRENT_MODE == RESTORE) {
        graal_isolate_t* isolate;
        graal_isolatethread_t *isolatethread = NULL;
        isolate_abi_t abi;
        restore_svm(FPATH, "metadata.snap", "memory.snap", &abi, &isolate);
        abi.graal_attach_thread(isolate, &isolatethread);
        run_entrypoint(&abi, isolate, isolatethread, CONC, ITERS, fin, fout, FOUT_LEN);
        // Note: from manual (https://www.graalvm.org/22.1/reference-manual/native-image/C-API/):
        // 'no code may still be executing in the isolate thread's context.' Since we cannot
        // guarantee that threads may be left behind, it is not safe to detach.
        //abi.graal_detach_thread(isolatethread);
    } else if (CURRENT_MODE == CHECKPOINT) {
        checkpoint_svm(FPATH, "metadata.snap", "memory.snap", SEED, CONC, ITERS, fin, fout, FOUT_LEN, NULL, NULL);
    } else {
        graal_isolate_t* isolate;
        isolate_abi_t abi;
        run_svm(FPATH, CONC, ITERS, fin, fout, FOUT_LEN, &abi, &isolate);
    }

    fprintf(stdout, "function(%s) -> %s\n", fin, fout);

    // Flush any open streammed file.
    fflush(NULL);

    // Exit even if there are unfinished threads running in function code.
    exit(0);
}
