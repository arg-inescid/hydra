app = "$(shell pwd)/libhello.so"

libhello.so: src/main/java/com/hello_world/HelloWorld.java
	mkdir build &> /dev/null
	${JAVA_HOME}/bin/javac -d build src/main/java/com/hello_world/HelloWorld.java
	${JAVA_HOME}/bin/jar --create --file build/hw.jar --manifest src/main/resources/META-INF/MANIFEST.MF -C build com
	${JAVA_HOME}/bin/native-image -g -O0 -H:-AllowVMInternalThreads --language:js -cp build/hw.jar --shared -H:Name=libhello

all: libhello.so

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ../main checkpoint $(app) 2>&1

run-restore:
	setarch -R ../main restore $(app) 2>&1

run: all run-checkpoint run-restore

clean:
	rm -f *.txt *.so *.snap *.h *.debug
	rm -f -r sources build
