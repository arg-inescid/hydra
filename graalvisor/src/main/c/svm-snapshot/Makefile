#app = "test0-clib/libhello.so"
#app = "test1-staticvar/libhello.so"
app = "$(shell pwd)/test2-svm/libhello.so"
#app = "$(shell pwd)/test2-svm-gradle/libhello.so"
#app = "/home/rbruno/git/graalserverless/benchmarks/src/java/gv-hello-world/build/libhelloworld.so"
#app = "/home/rbruno/git/graalserverless/benchmarks/src/python/gv-hello-world/build/libhelloworld.so"
#app = "/home/rbruno/git/graalserverless/benchmarks/src/javascript/gv-hello-world/build/libhelloworld.so"
gcommands = -x gdb-commands.txt

all: *.h *.c
	gcc -g -c *.c
	gcc -g -o main *.o

tests:
	make -C test0-clib run
	make -C test1-staticvar run
	make -C test2-svm run
	make -C test2-svm-gradle run

run-normal:
	setarch -R ./main normal $(app) 2>&1

strace-normal:
	setarch -R strace -f ./main normal $(app) 2>&1

gdb-normal:
	setarch -R gdb $(gcommands) --args ./main normal $(app)

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ./main checkpoint $(app) 2>&1

run-restore:
	setarch -R ./main restore $(app) 2>&1

gdb-restore:
	setarch -R gdb $(gcommands) --args ./main restore $(app)

strace-restore:
	setarch -R strace -f ./main restore $(app) 2>&1

clean:
	rm -f *.o main *.snap
	make -C test0-clib clean
	make -C test1-staticvar clean
	make -C test2-svm clean
	make -C test2-svm-gradle clean
