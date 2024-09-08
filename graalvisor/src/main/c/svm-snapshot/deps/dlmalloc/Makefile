dlflags = -DHAVE_MORECORE=0 -DUSE_DL_PREFIX

all: malloc.c
	gcc -O3 -Wall $(dlflags) -Wno-maybe-uninitialized  -c malloc.c

clean:
	rm -f *.o
