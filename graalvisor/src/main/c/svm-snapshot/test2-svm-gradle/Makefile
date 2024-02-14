app = "$(shell pwd)/libhello.so"

libhello.so: src/main/java/com/hello_world/HelloWorld.java
	./gradlew clean shadowJar assemble
	${JAVA_HOME}/bin/native-image \
		-g -O0 -H:-AllowVMInternalThreads \
		-cp build/libs/test2-svm-gradle-1.0-all.jar \
		--shared \
		-H:Name=libhello

all: libhello.so

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ../main checkpoint $(app) 2>&1 | tee checkpoint.log

run-restore:
	setarch -R ../main restore $(app) 2>&1 | tee restore.log

run: all run-checkpoint run-restore

clean:
	rm -f *.txt *.so *.snap *.h *.debug
	rm -f -r build sources bin
