#app = "$(shell pwd)/test0-clib/libhello.so"
#app = "$(shell pwd)/test1-staticvar/libhello.so"
#app = "$(shell pwd)/test2-svm/libhello.so"
#app = "$(shell pwd)/test2-svm-jar/libhello.so"
#app = "$(shell pwd)/test2-svm-gradle/libhello.so"
#app = "$(shell pwd)/test3-svm-truffle-js/libhello.so"
#app = "$(shell pwd)/test3-svm-truffle-py/libhello.so"
#app = "$(shell pwd)/test4-svm-exception/libhello.so"
#app = "$(shell pwd)/test5-svm-truffle-cached-js/libhello.so"
#app = "$(shell pwd)/test5-svm-truffle-cached-py/libhello.so"
#app = "$(shell pwd)/test6-cthreads/libhello.so"
app = "$(shell pwd)/test7-svm-truffle-function-py/libcompression.so"
#app = "$(shell pwd)/test8-cthreads-syscall/libhello.so"
#app = "$(shell pwd)/test9-cthreads-futex/libhello.so"

benchmarks_home = ${ARGO_HOME}/benchmarks
#app = "$(benchmarks_home)/src/java/gv-hello-world/build/libhelloworld.so"
#app = "$(benchmarks_home)/src/java/gv-file-hashing/build/libfilehashing.so"
# TODO - classify (process)
#app = "$(benchmarks_home)/src/java/gv-httprequest/build/libhttprequest.so"
# TODO - videoprocessing (process)

#app = "$(benchmarks_home)/src/javascript/gv-hello-world/build/libhelloworld.so"
# Note: sometimes is hangs with high concurrency due to network connections.
#app = "$(benchmarks_home)/src/javascript/gv-dynamic-html/build/libdynamichtml.so"
# Note: thumbnail calls native code and it also leaves files behind. We do not support that.
#app = "$(benchmarks_home)/src/javascript/gv-thumbnail/build/libthumbnail.so"
#app = "$(benchmarks_home)/src/javascript/gv-uploader/build/libuploader.so"

#app = "$(benchmarks_home)/src/python/gv-hello-world/build/libhelloworld.so"
# Seems to be crashing.
#app = "$(benchmarks_home)/src/python/gv-compression/build/libcompression.so"
#app = "$(benchmarks_home)/src/python/gv-uploader/build/libuploader.so"
#app = "$(benchmarks_home)/src/python/gv-mst/build/libmst.so"
#app = "$(benchmarks_home)/src/python/gv-pagerank/build/libpr.so"
#app = "$(benchmarks_home)/src/python/gv-bfs/build/libbfs.so"
#app = "$(benchmarks_home)/src/python/gv-dna/build/libdna.so"
#app = "$(benchmarks_home)/src/python/gv-dynamic-html/build/libdynamichtml.so"
# Note: fails on restore w/ segfault.
#app = "$(benchmarks_home)/src/python/gv-thumbnail/build/libthumbnail.so"
# Note both of these use sub-processes.
#gv_python_videoprocessing (process?)
# TODO - classify (process)

gcommands = -x gdb-commands.txt

all: *.h *.c
	gcc -g -Wall -c *.c
	gcc -g -Wall -o main *.o

tests: all
	make -C test0-clib test
	make -C test1-staticvar test
	make -C test2-svm test
	make -C test2-svm-jar test
	make -C test2-svm-gradle test
	make -C test3-svm-truffle-js test
	make -C test3-svm-truffle-py test
	make -C test4-svm-exception test
	make -C test5-svm-truffle-cached-js test
	make -C test5-svm-truffle-cached-py test
	make -C test6-cthreads test
	make -C test7-svm-truffle-function-py test
	make -C test8-cthreads-syscall test
	make -C test9-cthreads-futex test

run-normal: all
	setarch -R ./main normal $(app) 2>&1 | tee normal.log

strace-normal: all
	setarch -R strace -f ./main normal $(app) 2>&1 | tee strace-normal.log

gdb-normal: all
	setarch -R gdb $(gcommands) --args ./main normal $(app)

gdb-checkpoint: all
	setarch -R gdb $(gcommands) --args ./main checkpoint $(app) | tee gdb-checkpoint.log

run-checkpoint: all
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ./main checkpoint $(app) 2>&1 | tee checkpoint.log

strace-checkpoint: all
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R strace  -f ./main checkpoint $(app) 2>&1 | tee strace-checkpoint.log

run-restore: all
	setarch -R ./main restore $(app) 2>&1 | tee restore.log

run: run-checkpoint run-restore

run-perf: all
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R /bin/time -v ./main normal $(app) 2>&1 | tee normal.log
	setarch -R /bin/time -v ./main checkpoint $(app) 2>&1 | tee checkpoint.log
	setarch -R /bin/time -v ./main restore $(app) 2>&1 | tee restore.log

gdb-restore: all
	setarch -R gdb $(gcommands) --args ./main restore $(app)

strace-restore: all
	setarch -R strace -f ./main restore $(app) 2>&1 | tee strace-restore.log

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
	make -C test5-svm-truffle-cached-js clean
	make -C test5-svm-truffle-cached-py clean
	make -C test6-cthreads clean
	make -C test7-svm-truffle-function-py clean
	make -C test8-cthreads-syscall clean
	make -C test9-cthreads-futex clean
