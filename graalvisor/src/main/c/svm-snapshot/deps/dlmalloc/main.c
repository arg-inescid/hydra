#include <stdio.h>
#include <stdlib.h>
#include "cr_malloc.h"

int main(int argc, char** argv) {
    int* buffer = malloc(32 * sizeof(int));
    free(buffer);
    printf("OLA!! %d\n", 32);
    checkpoint_mspace();
    return 0;
}