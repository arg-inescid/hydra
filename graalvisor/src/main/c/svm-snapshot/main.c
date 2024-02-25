// Important to load some headers such as sched.h.
#define _GNU_SOURCE

#include "svm-snapshot.h"
#include <stdio.h>
#include <stdlib.h>

enum EXECUTION_MODE { NORMAL, CHECKPOINT, RESTORE };

// Wether we are checkpointing or restoreing.
enum EXECUTION_MODE CURRENT_MODE = NORMAL;

void usage_exit() {
    fprintf(stderr, "Syntax: main <normal|restore> <path to native image app library>\n");
    fprintf(stderr, "Syntax: main checkpoint <path to native image app library> [seed]\n");
    exit(1);
}

char* init_args(int argc, char** argv) {
    if (argc < 3) {
        usage_exit();
    } else {
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
    }
    return argv[2];
}

int main(int argc, char** argv) {
    // Check args and get function path.
    char* function_path = init_args(argc, argv);

    // Disable buffering for stdout.
    setvbuf(stdout, NULL, _IONBF, 0);

    // If in restore mode, start by restoring from the snapshot.
    if (CURRENT_MODE == RESTORE) {
        graal_isolate_t* isolate;
        graal_isolatethread_t *isolatethread = NULL;
        isolate_abi_t abi;
        restore_svm("metadata.snap", "memory.snap", &abi, &isolate);
        abi.graal_attach_thread(isolate, &isolatethread);
        run_entrypoint(&abi, isolate, isolatethread);
        abi.graal_detach_thread(isolatethread);
    } else if (CURRENT_MODE == CHECKPOINT) {
        size_t seed = argc == 4 ? atoi(argv[3]) : 0;
        checkpoint_svm(function_path, NULL, seed, "metadata.snap", "memory.snap", NULL, NULL);
    } else {
        graal_isolate_t* isolate;
        isolate_abi_t abi;
        run_svm(function_path, &abi, &isolate);
    }

    // Flush any open streammed file.
    fflush(NULL);

    // Exit even if there are unfinished threads running in function code.
    exit(0);
}
