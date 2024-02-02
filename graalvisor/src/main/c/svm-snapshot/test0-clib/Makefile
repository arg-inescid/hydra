app = "$(shell pwd)/libhello.so"

all:
	gcc -O0 -g -c -fPIC hello.c
	gcc -O0 -g -shared -o libhello.so *.o

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ../main checkpoint $(app) 2>&1

run-restore:
	setarch -R ../main restore $(app) 2>&1

run: all run-checkpoint run-restore

clean:
	rm *.txt *.o *.so *.snap