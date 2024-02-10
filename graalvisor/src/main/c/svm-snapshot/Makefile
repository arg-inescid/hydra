#app = "$(shell pwd)/test0-clib/libhello.so"
#app = "$(shell pwd)/test1-staticvar/libhello.so"
#app = "$(shell pwd)/test2-svm/libhello.so"
#app = "$(shell pwd)/test2-svm-jar/libhello.so"
#app = "$(shell pwd)/test2-svm-gradle/libhello.so"
#app = "$(shell pwd)/test3-svm-truffle-js/libhello.so"
app = "$(shell pwd)/test3-svm-truffle-py/libhello.so"

gcommands = -x gdb-commands.txt

all: *.h *.c
	gcc -g -Wall -c *.c
	gcc -g -Wall -o main *.o

tests: all
	make -C test0-clib run
	make -C test1-staticvar run
	make -C test2-svm run
	make -C test2-svm-jar run
	make -C test2-svm-gradle run
	make -C test3-svm-truffle-js run
	make -C test3-svm-truffle-py run

run-normal:
	setarch -R ./main normal $(app) 2>&1

strace-normal:
	setarch -R strace -f ./main normal $(app) 2>&1

gdb-normal:
	setarch -R gdb $(gcommands) --args ./main normal $(app)

gdb-checkpoint:
	setarch -R gdb $(gcommands) --args ./main checkpoint $(app)

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ./main checkpoint $(app) 2>&1 | tee checkpoint.log

strace-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R strace -f ./main checkpoint $(app) 2>&1

run-restore:
	setarch -R ./main restore $(app) 2>&1 | tee restore.log

run: run-checkpoint run-restore

gdb-restore:
	setarch -R gdb $(gcommands) --args ./main restore $(app)

strace-restore:
	setarch -R strace -f ./main restore $(app) 2>&1

clean:
	rm -f *.o main *.snap
	make -C test0-clib clean
	make -C test1-staticvar clean
	make -C test2-svm clean
	make -C test2-svm-jar clean
	make -C test2-svm-gradle clean
	make -C test3-svm-truffle-js clean
	make -C test3-svm-truffle-py clean
