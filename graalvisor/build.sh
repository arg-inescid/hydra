#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
GRAALVISOR_HOME=$DIR/build/native-image
GRAALVISOR_JAR=$DIR/build/libs/graalvisor-1.0-all.jar
GREEN='\033[0;32m'
NC='\033[0m' # No Color

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
            $LINKER_OPTIONS
            -H:NativeLinkerOption=-lpthread
            -H:NativeLinkerOption="$LIB_DIR/lazyisolation.o"
            -H:NativeLinkerOption="$LIB_DIR/shared_queue.o"
            -H:NativeLinkerOption="$LIB_DIR/filters.o""
        NSI_FLAGS="$NSI_FLAGS -DLAZY_ISOLATION"
    fi
}

function build_svm_snapshot {
    # Build svm snapshot sub-project.
    make -C $SNAP_DIR
    # We don't want to include the main (which is used for testing).
    rm $SNAP_DIR/main.o
    # Add all object files to the list of objects to be included in the final binary.
    for f in $SNAP_DIR/*.o $SNAP_DIR/deps/printf/printf.o $SNAP_DIR/deps/dlmalloc/malloc.o;
    do
        LINKER_OPTIONS="$LINKER_OPTIONS -H:NativeLinkerOption="$f""
    done
}

function build_network_isolation {
    gcc -c -I"$NET_DIR" -o $LIB_DIR/network-isolation.o $NET_DIR/network-isolation.c
    LINKER_OPTIONS="$LINKER_OPTIONS -H:NativeLinkerOption="$LIB_DIR/network-isolation.o""
}

function build_nsi {
    HEADER_DIR=$DIR/build/generated/sources/headers/java/main
    C_DIR=$DIR/src/main/c
    LAZY_DIR=$C_DIR/lazyisolation/src
    NET_DIR=$C_DIR/network-isolation/src
    SNAP_DIR=$C_DIR/svm-snapshot
    LIB_DIR=$DIR/build/libs
    # TODO - make these optional
    # Comment/Uncomment to disable/enable lazy isolation.
    #build_lazyisolation
    build_network_isolation
    build_svm_snapshot
    gcc -c \
        -I"$JAVA_HOME/include" \
        -I"$JAVA_HOME/include/linux" \
        -I"$HEADER_DIR" \
        -I"$LAZY_DIR" \
        -I"$NET_DIR" \
        -I"$SNAP_DIR" \
        -o $LIB_DIR/NativeSandboxInterface.o \
        $C_DIR/NativeSandboxInterface.c \
        $NSI_FLAGS
    ar rcs $LIB_DIR/libNativeSandboxInterface.a $LIB_DIR/NativeSandboxInterface.o
}

function build_ni {
    mkdir -p $GRAALVISOR_HOME &> /dev/null
    cd $GRAALVISOR_HOME
    if [[ $JAVA_VERSION == *"17"* ]]; then
        JAVA_17_OPTS="$JAVA_17_OPTS --add-exports org.graalvm.nativeimage.builder/com.oracle.svm.core.os=ALL-UNNAMED"
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
        $LIBC_OPTION \
        --no-fallback \
        --install-exit-handlers \
        --enable-url-protocols=http \
        --initialize-at-run-time=com.oracle.svm.graalvisor.utils.JsonUtils \
        $LINKER_OPTIONS \
        -H:CLibraryPath=$LIB_DIR \
        $JAVA_17_OPTS \
        --features=org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterfaceFeature \
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
else  # Build native image locally (inside container or directly on host).
    read -p "Use musl libc? (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        export PATH=$ARGO_HOME/resources/x86_64-linux-musl-native/bin:$PATH
        LIBC_OPTION="--libc=musl"
    fi

    echo -e "${GREEN}Building graalvisor jar...${NC}"
    ./gradlew clean shadowJar
    echo -e "${GREEN}Building graalvisor jar... done!${NC}"

    echo -e "${GREEN}Building graalvisor native sandbox interface...${NC}"
    build_nsi
    echo -e "${GREEN}Building graalvisor native sandbox interface... done!${NC}"

    echo -e "${GREEN}Building graalvisor Native Image...${NC}"
    build_ni
    echo -e "${GREEN}Building graalvisor Native Image... done!${NC}"
fi
