app = "$(shell pwd)/libcompression.so"

libcompression.so: src/main/java/com/compression/Compression.java
		#--enable-url-protocols=http 
	./gradlew clean shadowJar assemble
	${JAVA_HOME}/bin/native-image \
		-g -O0 -H:-AllowVMInternalThreads \
		-cp build/libs/compression-1.0-all.jar \
		-Dcom.oracle.svm.graalvisor.polyglotengine.language=python \
		-Dcom.oracle.svm.graalvisor.polyglotengine.entrypoint=main \
		-Dcom.oracle.svm.graalvisor.polyglotengine.source=$(shell pwd)/src/main/python/main.py \
		--initialize-at-build-time=com.compression.Compression \
		--language:python \
		--shared \
		-H:Name=libcompression

all: libcompression.so

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	setarch -R ../main checkpoint $(app) 2>&1 | tee checkpoint.log

run-restore:
	setarch -R ../main restore $(app) 2>&1 | tee restore.log

run: all run-checkpoint run-restore

clean:
	rm -f *.txt *.so *.snap *.h *.debug
	rm -f -r build sources bin resources
