all: main.c
	gcc -g -c main.c
	gcc -g -o main main.o

run:
	setarch -R ./main

strace:
	strace -f ./main