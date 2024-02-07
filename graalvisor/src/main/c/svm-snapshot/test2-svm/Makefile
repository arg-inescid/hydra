app = "$(shell pwd)/libhello.so"

libhello.so: HelloWorld.java
	${JAVA_HOME}/bin/javac HelloWorld.java
	${JAVA_HOME}/bin/native-image \
		-g -O0 -H:-AllowVMInternalThreads \
		--shared \
		-H:Name=libhello \
		HelloWorld

all: libhello.so

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ../main checkpoint $(app) 2>&1

run-restore:
	setarch -R ../main restore $(app) 2>&1

run: all run-checkpoint run-restore

clean:
	rm *.txt *.so *.snap *.h *.debug *.class
	rm -r sources
