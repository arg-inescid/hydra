#include <stdio.h>
#include <stdlib.h>
#include "cr_malloc.h"

int main(int argc, char** argv) {
    int* buffer = malloc(32 * sizeof(int));
    printf("OLA!! %d\n", 32);
    int* buffer2 = malloc(32 * sizeof(int));
    free(buffer);
    int* buffer3 = malloc(64 * 1000 * sizeof(int));
    inspect_chunks();
    free(buffer3);
    inspect_chunks();
    // inspect_segments();
 
    int* zero = calloc(32, sizeof(int));
    printf("calloc ptr[10] contents: %d\n", zero[10]);
    int* extended = realloc(zero, 2048);
    printf("realloc ptr[2035] contents: %d\n", extended[2035]);

    checkpoint_mspace();

    printf("\n\n");
    print_mspace();
    printf("\n\n");

    free(buffer2);
    inspect_chunks();
    return 0;
}
