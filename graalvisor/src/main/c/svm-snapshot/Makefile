all: main.h main.c list.h list.c cr.h cr.c
	gcc -g -c *.c
	gcc -g -o main *.o

run:
	setarch -R ./main 2>&1

clean:
	rm -f snapshot.mem
	rm -f snapshot.meta

strace:
	strace -f ./main 2>&1
