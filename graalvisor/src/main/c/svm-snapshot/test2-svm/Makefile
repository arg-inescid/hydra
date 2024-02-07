app = "$(shell pwd)/libhello.so"

all:
	./gradlew clean shadowJar assemble
	${JAVA_HOME}/bin/native-image \
		-g -O0 -H:-AllowVMInternalThreads \
		-cp build/libs/test2-svm-1.0-all.jar \
		--shared \
		-H:Name=libhello

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ../main checkpoint $(app) 2>&1

run-restore:
	setarch -R ../main restore $(app) 2>&1

run: all run-checkpoint run-restore

clean:
	rm *.txt *.so *.snap *.h *.debug
	rm -r build sources
