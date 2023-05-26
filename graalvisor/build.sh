#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
GRAALVISOR_HOME=$DIR/build/native-image
GRAALVISOR_JAR=$DIR/build/libs/graalvisor-1.0-all.jar
GREEN='\033[0;32m'
NC='\033[0m' # No Color

function build_nsi {
	HEADER_DIR=$DIR/build/generated/sources/headers/java/main
	C_DIR=$DIR/src/main/c
	LIB_DIR=$DIR/build/libs
	gcc -c -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$HEADER_DIR" -o $LIB_DIR/NativeSandboxInterface.o $C_DIR/NativeSandboxInterface.c
	ar rcs $LIB_DIR/libNativeSandboxInterface.a $LIB_DIR/NativeSandboxInterface.o
}

function build_ni {
    mkdir -p $GRAALVISOR_HOME &> /dev/null
    cd $GRAALVISOR_HOME
    $JAVA_HOME/bin/native-image \
        --no-fallback \
        --enable-url-protocols=http \
        --initialize-at-run-time=com.oracle.svm.graalvisor.utils.JsonUtils \
	-H:CLibraryPath=$LIB_DIR \
        -DGraalVisorHost \
        -Dcom.oracle.svm.graalvisor.libraryPath=$DIR/build/resources/main/com.oracle.svm.graalvisor.headers \
        $LANGS \
        -cp $GRAALVISOR_JAR \
        org.graalvm.argo.graalvisor.Main \
        polyglot-proxy \
        -H:+ReportExceptionStackTraces \
        -H:ConfigurationFileDirectories=$DIR/ni-agent-config/native-image
}

function build_vm_image {
    $DIR/../niuk/build_vm_image.sh $JAVA_HOME $GRAALVISOR_HOME/polyglot-proxy $GRAALVISOR_HOME/polyglot-proxy.img
}

function build_graalvisor_container_image {
    $DIR/../niuk/build_graalvisor_container_image.sh $JAVA_HOME $GRAALVISOR_HOME/polyglot-proxy
}

function build_hotspot_container_image {
    $DIR/../niuk/build_hotspot_container_image.sh $JAVA_HOME $GRAALVISOR_JAR
}

if [ -z "$JAVA_HOME" ]
then
        echo "Please set JAVA_HOME first. It should be a GraalVM with native-image available."
        exit 1
fi

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

EXECUTION_ENVIRONMENT=$1
BUILD_VM_IMAGE="false"
BUILD_GRAALVISOR_CONTAINER_IMAGE="false"
BUILD_HOTSPOT_CONTAINER_IMAGE="false"
if [[ "$EXECUTION_ENVIRONMENT" != "in-container" ]]; then
    read -p "Build vm image (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        BUILD_VM_IMAGE="true"
    fi

    read -p "Build Graalvisor container image (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        BUILD_GRAALVISOR_CONTAINER_IMAGE="true"
    fi

    read -p "Build HotSpot container images (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        BUILD_HOTSPOT_CONTAINER_IMAGE="true"
    fi
fi

if [[ "$BUILD_VM_IMAGE" == "true" ]] || [[ "$BUILD_GRAALVISOR_CONTAINER_IMAGE" == "true" ]]
then  # Build native image inside Docker container.
    docker run -it -v $JAVA_HOME:/jvm -v $ARGO_HOME:/argo --rm argo-builder /argo/graalvisor/build.sh in-container
    sudo chown -R $USER:$USER build
else  # Build native image locally.
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

if [[ "$BUILD_VM_IMAGE" == "true" ]]
then
    echo -e "${GREEN}Building vm image...${NC}"
    build_vm_image
    echo -e "${GREEN}Building vm image... done!${NC}"
fi

if [[ "$BUILD_GRAALVISOR_CONTAINER_IMAGE" == "true" ]]
then
    echo -e "${GREEN}Building Graalvisor container image...${NC}"
    build_graalvisor_container_image
    echo -e "${GREEN}Building Graalvisor container image... done!${NC}"
fi

if [[ "$BUILD_HOTSPOT_CONTAINER_IMAGE" == "true" ]]
then
    echo -e "${GREEN}Building HotSpot container images...${NC}"
    build_hotspot_container_image
    echo -e "${GREEN}Building HotSpot container images... done!${NC}"
fi
