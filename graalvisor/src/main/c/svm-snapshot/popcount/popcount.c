#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/mman.h>

size_t popcount(char* buffer, size_t count) {
    size_t result = 0;
    for (int i = 0; i < count / sizeof(unsigned int); i++) {
        result += __builtin_popcount(*(unsigned int*)buffer);
        buffer += sizeof(unsigned int);
    }
    return result;
}

size_t read_page(int fd, char* buffer) {
    size_t n = 0;
    size_t written = 0;
    while (n = read(fd, buffer + written, 4096 - written)) {
        if (n < 0) {
            if (errno == EINTR || errno == EAGAIN) {
                continue;
            } else {
                fprintf(stderr, "Could not read() data\n");
            }
        } else {
            written += n;
        }
    }
    return written;
}

int main(int argc, char** argv) {
    size_t n = 0;

    char* buffer = (char*) malloc(4096);
    if (buffer == NULL) {
        fprintf(stderr, "failed to malloc\n");
    }
    
    int fd = open(argv[1], O_RDONLY);
    if (fd < 0) {
        fprintf(stderr, "failed to open %s\n", argv[1]);
        return 1;
    }
    
    read_page(fd, buffer);
    fprintf(stderr, "popcount(4k from %s) = %lu\n", argv[1], popcount(buffer, 4096));
    read_page(fd, buffer);
    fprintf(stderr, "popcount(4k from %s) = %lu\n", argv[1], popcount(buffer, 4096));
    read_page(fd, buffer);
    fprintf(stderr, "popcount(4k from %s) = %lu\n", argv[1], popcount(buffer, 4096));
    read_page(fd, buffer);
    fprintf(stderr, "popcount(4k from %s) = %lu\n", argv[1], popcount(buffer, 4096));
    read_page(fd, buffer);
    fprintf(stderr, "popcount(4k from %s) = %lu\n", argv[1], popcount(buffer, 4096));
    int pc = 0;
    for(int i = 0; i < 21; i++, pc += popcount(buffer, 4096)) {
        read_page(fd, buffer);
    }
    fprintf(stderr, "popcount(21 * 4k from %s) = %lu\n", argv[1], pc);   

    close(fd);
    return 0;
}