dlflags = -DHAVE_MORECORE=0 -DUSE_DL_PREFIX # -DONLY_MSPACES

all: malloc.c malloc.h
	gcc -O0 -g3 -Wall $(dlflags) -Wno-maybe-uninitialized  -c malloc.c

test: all cr_malloc.c cr_malloc.h main.c
	gcc -O0 -g3 -Wall $(dlflags) -Wno-maybe-uninitialized  -c cr_malloc.c
	gcc -O0 -g3 -Wall $(dlflags) -Wno-maybe-uninitialized  -c main.c
	gcc -O0 -g3 -Wall -o main main.o cr_malloc.o malloc.o ../printf/printf.o ../../cr_logger.o

clean:
	rm -f *.o
