#app = "$(shell pwd)/test0-clib/libhello.so"
#app = "$(shell pwd)/test1-staticvar/libhello.so"
#app = "$(shell pwd)/test2-svm/libhello.so"
#app = "$(shell pwd)/test2-svm-jar/libhello.so"
#app = "$(shell pwd)/test2-svm-gradle/libhello.so"
#app = "$(shell pwd)/test3-svm-truffle-js/libhello.so"
#app = "$(shell pwd)/test3-svm-truffle-py/libhello.so"
#app = "$(shell pwd)/test4-svm-exception/libhello.so"
app = "$(shell pwd)/test5-svm-truffle-cached-js/libhello.so"

gcommands = -x gdb-commands.txt

all: *.h *.c
	gcc -g -Wall -c *.c
	gcc -g -Wall -o main *.o

tests: all
	make -C test0-clib run           | tee test0-clibc.log          | tail -n2
	make -C test1-staticvar run      | tee test1-staticvar.log      | tail -n2
	make -C test2-svm run            | tee test2-svm.log            | tail -n2
	make -C test2-svm-jar run        | tee test2-svm-jar.log        | tail -n2
	make -C test2-svm-gradle run     | tee test2-svm-gradle.log     | tail -n2
	make -C test3-svm-truffle-js run | tee test3-svm-truffle-js.log | tail -n2
	make -C test3-svm-truffle-py run | tee test3-svm-truffle-py.log | tail -n2
	make -C test4-svm-exception run  | tee test4-svm-exception.log  | tail -n2

run-normal:
	setarch -R ./main normal $(app) 2>&1 | tee normal.log

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
	rm -f *.o main *.snap *.log

clean-tests:
	make -C test0-clib clean
	make -C test1-staticvar clean
	make -C test2-svm clean
	make -C test2-svm-jar clean
	make -C test2-svm-gradle clean
	make -C test3-svm-truffle-js clean
	make -C test3-svm-truffle-py clean
	make -C test4-svm-exception clean
