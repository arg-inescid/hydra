#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
GRAALVISOR_HOME=$DIR/build/native-image
GRAALVISOR_JAR=$DIR/build/libs/graalvisor-1.0-all.jar
GREEN='\033[0;32m'
NC='\033[0m' # No Color

function build_memisolation {
	release=$(uname -r)
	major_version=${release%%.*}
	release=${release#*.}
	minor_version=${release%%.*}
	CFLAGS="-Wall -g -fno-inline -fPIC -shared \
            -I"$ERIM_HOME/src/erim" \
            -I"$ERIM_HOME/src/common" \
            -DERIM_SWAP_STACKS"
            #-DEAGER_LOAD \
            #-DSEC_DBG \
            #-DERIM_DBG \
    LIBRARIES="$ERIM_HOME/bin/common/libswscommon.a $ERIM_HOME/bin/erim/liberim.a"

	if [ $major_version -ge 5 ] && [ $minor_version -ge 10 ]; then
	    	gcc -c -I"$MEM_DIR" $CFLAGS -o $LIB_DIR/appmap.o $MEM_DIR/utils/appmap.c
	    	gcc -c -I"$MEM_DIR" $CFLAGS -o $LIB_DIR/helpers.o $MEM_DIR/helpers/helpers.c
        	gcc -c -I"$MEM_DIR" $CFLAGS -o $LIB_DIR/memisolation.o $MEM_DIR/memisolation.c
            gcc -c -I"$MEM_DIR" $CFLAGS -o $LIB_DIR/preload.o $PRELOAD_DIR/preload.c
            gcc $CFLAGS -o $LIB_DIR/libtest.so $LIB_DIR/preload.o $LIB_DIR/helpers.o $LIB_DIR/appmap.o $LIB_DIR/memisolation.o 
            gcc $CFLAGS -o $LIB_DIR/libpreload.so $LIB_DIR/appmap.o $LIB_DIR/helpers.o $LIB_DIR/memisolation.o -lm $LIBRARIES
            
            MEM_FLAGS="-DMEM_ISOLATION"
    fi
}

function build_lazyisolation {
	release=$(uname -r)
	major_version=${release%%.*}
	release=${release#*.}
	minor_version=${release%%.*}

	if [ $major_version -ge 5 ] && [ $minor_version -ge 10 ]; then
        	gcc -c -I"$LAZY_DIR" -o $LIB_DIR/lazyisolation.o $LAZY_DIR/lazyisolation.c
	    	gcc -c -I"$LAZY_DIR" -o $LIB_DIR/shared_queue.o $LAZY_DIR/shared_queue.c
	    	gcc -c -I"$LAZY_DIR" -o $LIB_DIR/filters.o $LAZY_DIR/filters.c
        	LINKER_OPTIONS="
            		-H:NativeLinkerOption=-lpthread
            		-H:NativeLinkerOption="$LIB_DIR/lazyisolation.o"
            		-H:NativeLinkerOption="$LIB_DIR/shared_queue.o"
	            	-H:NativeLinkerOption="$LIB_DIR/filters.o""
            	LAZY_FLAGS="-DLAZY_ISOLATION"
    	fi
}

function build_nsi {
	HEADER_DIR=$DIR/build/generated/sources/headers/java/main
	C_DIR=$DIR/src/main/c
	LAZY_DIR=$DIR/src/main/c/lazyisolation/src
    MEM_DIR=$DIR/src/main/c/memisolation/src
    PRELOAD_DIR=$DIR/src/main/c/ldpreload
	LIB_DIR=$DIR/build/libs
	build_lazyisolation
    build_memisolation
	gcc -c -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$HEADER_DIR" -I"$LAZY_DIR" -I"$MEM_DIR" -o $LIB_DIR/NativeSandboxInterface.o $C_DIR/NativeSandboxInterface.c $LAZY_FLAGS $MEM_FLAGS
	ar rcs $LIB_DIR/libNativeSandboxInterface.a $LIB_DIR/NativeSandboxInterface.o
}

function build_ni {
    mkdir -p $GRAALVISOR_HOME &> /dev/null
    cd $GRAALVISOR_HOME
    if [[ $JAVA_VERSION == *"17"* ]]; then
        JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.core.posix=ALL-UNNAMED"
	JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.core.posix.headers=ALL-UNNAMED"
        JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.core.c=ALL-UNNAMED"
        JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.core.c.function=ALL-UNNAMED"
        JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED"
        JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED"
        JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED"
        JAVA_17_OPTS="$JAVA_17_OPTS --add-opens=java.base/java.io=ALL-UNNAMED"
    fi
    $JAVA_HOME/bin/native-image \
        --no-fallback \
        --enable-url-protocols=http \
        --initialize-at-run-time=com.oracle.svm.graalvisor.utils.JsonUtils \
        $LINKER_OPTIONS \
        -H:CLibraryPath=$LIB_DIR \
	$JAVA_17_OPTS \
        --features=org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterfaceFeature \
        -DGraalVisorHost \
        -Dcom.oracle.svm.graalvisor.libraryPath=$DIR/build/resources/main/com.oracle.svm.graalvisor.headers \
        $LANGS \
        -cp $GRAALVISOR_JAR \
        org.graalvm.argo.graalvisor.Main \
        polyglot-proxy \
        -H:+ReportExceptionStackTraces \
        -H:ConfigurationFileDirectories=$DIR/ni-agent-config/native-image
}

if [ -z "$JAVA_HOME" ]
then
    echo "Please set JAVA_HOME first. It should be a GraalVM with native-image available."
    exit 1
else
    eval $(echo "export $(cat $JAVA_HOME/release | grep JAVA_VERSION=)")
    eval $(echo "export $(cat $JAVA_HOME/release | grep GRAALVM_VERSION=)")
fi

if [ -z "$ARGO_HOME" ]
then
    echo "Please set ARGO_HOME first. It should point to a checkout of github.com/graalvm/argo."
    exit 1
fi

cd "$DIR" || {
    echo "Redirection failed!"
    exit 1
}

EXECUTION_ENVIRONMENT=$1
if [[ "$EXECUTION_ENVIRONMENT" != "local" ]]
then  # Build native image inside Docker container.
    docker run -it -v $JAVA_HOME:/jvm -v $ARGO_HOME:/argo --rm argo-builder /argo/graalvisor/build.sh "local"
    sudo chown -R $(id -u -n):$(id -g -n) $ARGO_HOME/graalvisor/build
    sudo chown -R $(id -u -n):$(id -g -n) $ARGO_HOME/graalvisor-lib/build
else  # Build native image locally (inside container or directly on host).
    LANGS=""
    read -p "Javascript support (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        LANGS="$LANGS --language:js"
        echo "JavaScript support added!"
    fi

    read -p "Python support (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        LANGS="$LANGS --language:python"
        echo "Python support added!"
    fi

    echo -e "${GREEN}Building graalvisor-lib jar...${NC}"
    $ARGO_HOME/graalvisor-lib/build.sh
    echo -e "${GREEN}Building graalvisor-lib jar... done!${NC}"

    echo -e "${GREEN}Building graalvisor jar...${NC}"
    ./gradlew clean shadowJar javaProxy
    echo -e "${GREEN}Building graalvisor jar... done!${NC}"

    echo -e "${GREEN}Building graalvisor native sandbox interface...${NC}"
    build_nsi
    echo -e "${GREEN}Building graalvisor native sandbox interface... done!${NC}"

    echo -e "${GREEN}Building graalvisor Native Image...${NC}"
    build_ni
    echo -e "${GREEN}Building graalvisor Native Image... done!${NC}"
fi
