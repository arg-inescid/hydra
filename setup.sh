#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function install_graalvm {
    wget https://download.oracle.com/graalvm/17/archive/graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
    tar -vzxf graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
    mv graalvm-jdk-17.0.7+8.1 $ARGO_HOME/resources
    rm graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
    export JAVA_HOME=$ARGO_HOME/resources/graalvm-jdk-17.0.7+8.1
    echo "export JAVA_HOME=\$ARGO_HOME/resources/graalvm-jdk-17.0.7+8.1" >> $ARGO_HOME/env.sh
    echo "set -gx JAVA_HOME \$ARGO_HOME/resources/graalvm-jdk-17.0.7+8.1" >> $ARGO_HOME/env.fish
    echo "Added JAVA_HOME in $ARGO_HOME/env.sh"
}

function install_firecracker {
    wget https://github.com/firecracker-microvm/firecracker/releases/download/v1.1.0/firecracker-v1.1.0-x86_64.tgz
    tar -vzxf firecracker-v1.1.0-x86_64.tgz
    mv release-v1.1.0-x86_64 $ARGO_HOME/resources/firecracker-v1.1.0-x86_64
    ln -s $ARGO_HOME/resources/firecracker-v1.1.0-x86_64/firecracker-v1.1.0-x86_64 $ARGO_HOME/resources/firecracker-v1.1.0-x86_64/firecracker
    rm firecracker-v1.1.0-x86_64.tgz
    export PATH=$ARGO_HOME/resources/firecracker-v1.1.0-x86_64:$PATH
    echo "export PATH=\$ARGO_HOME/resources/firecracker-v1.1.0-x86_64:\$PATH" >> $ARGO_HOME/env.sh
    echo "set -gx PATH \$ARGO_HOME/resources/firecracker-v1.1.0-x86_64 \$PATH" >> $ARGO_HOME/env.fish
    echo "Added firecracker to path in $ARGO_HOME/env.sh"
}

function install_musl {
    wget https://more.musl.cc/10/x86_64-linux-musl/x86_64-linux-musl-native.tgz
    wget https://zlib.net/current/zlib.tar.gz
    tar -vzxf x86_64-linux-musl-native.tgz
    tar -vzxf zlib.tar.gz
    mv x86_64-linux-musl-native $ARGO_HOME/resources/x86_64-linux-musl-native
    mv zlib-1.3.1 $ARGO_HOME/resources/
    rm x86_64-linux-musl-native.tgz
    rm zlib.tar.gz

    export PATH=$ARGO_HOME/resources/x86_64-linux-musl-native/bin:$PATH
    echo "export PATH=\$ARGO_HOME/resources/x86_64-linux-musl-native/bin:\$PATH" >> $ARGO_HOME/env.sh
    echo "set -gx PATH \$ARGO_HOME/resources/x86_64-linux-musl-native/bin \$PATH" >> $ARGO_HOME/env.fish
    echo "Added musl to path in $ARGO_HOME/env.sh"

    CC=$ARGO_HOME/resources/x86_64-linux-musl-native/bin/gcc
    cd $ARGO_HOME/resources/zlib-1.3.1
    ./configure --prefix=$ARGO_HOME/resources/x86_64-linux-musl-native --static
    make
    make install
    cd - &> /dev/null
}

export ARGO_HOME=$(DIR)
if [ ! -f $ARGO_HOME/env.sh ]; then
    export WORK_DIR=$ARGO_HOME/tmp
    echo "export ARGO_HOME=$ARGO_HOME"     >> $ARGO_HOME/env.sh
    echo "export WORK_DIR=\$ARGO_HOME/tmp"  >> $ARGO_HOME/env.sh
    echo "set -gx ARGO_HOME $ARGO_HOME"    >> $ARGO_HOME/env.fish
    echo "set -gx WORK_DIR \$ARGO_HOME/tmp" >> $ARGO_HOME/env.fish
    echo "Added ARGO_HOME to $ARGO_HOME/env.sh"
    echo "Added WORK_DIR to $ARGO_HOME/env.sh"
else
    source $ARGO_HOME/env.sh
fi

if [ -z "${JAVA_HOME}" ]; then
    echo "JAVA_HOME is not defined!"
    read -p "Install GraalVM in $ARGO_HOME/resources? (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        install_graalvm
    fi
fi

if ! grep -q GRAALVM_VERSION $JAVA_HOME/release; then
    echo "Error: JAVA_HOME does not point to a GraalVM distrubution: $JAVA_HOME"
    exit 1
else
    eval $(echo "export $(cat $JAVA_HOME/release | grep JAVA_VERSION=)")
    eval $(echo "export $(cat $JAVA_HOME/release | grep GRAALVM_VERSION=)")
fi

echo "ARGO_HOME = $ARGO_HOME"
echo "JAVA_HOME = $JAVA_HOME"
echo "WORK_DIR = $WORK_DIR"
echo "JAVA_VERSION = $JAVA_VERSION"
echo "GRAALVM_VERSION = $GRAALVM_VERSION"

if ! command -v firecracker &> /dev/null
then
    echo "WARNING: firecracker could not be found!"
    read -p "Install Firecracker microVM in $ARGO_HOME/resources? (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        install_firecracker
    fi
fi

if ! command -v x86_64-linux-musl-gcc &> /dev/null
then
    echo "WARNING: musl could not be found!"
    read -p "Install musl in $ARGO_HOME/resources? (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        install_musl
    fi
fi

if ! command -v docker &> /dev/null
then
    echo "WARNING: docker could not be found!"
    echo "If you plan on experimenting with Graalvisor's container backend then you should install Docker."
    GRAALVISOR_BUILD_MODE="local"
fi

if [ ! -f $ARGO_HOME/benchmarks/.git ];
then
    echo "Cloning benchmarks git module..."
    git pull --recurse-submodules
    echo "Cloning benchmarks git module... done!"
fi

if [ ! -f $ARGO_HOME/resources/hello-vmlinux.bin ];
then
    echo "Downloading linux kernel image..."
    curl -fsSL -o $ARGO_HOME/resources/hello-vmlinux.bin https://s3.amazonaws.com/spec.ccfc.min/img/hello/kernel/hello-vmlinux.bin
    echo "Downloading linux kernel image... done!"
fi

read -p "Build lambda manager? (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $ARGO_HOME/lambda-manager/build.sh
fi

read -p "Build builder container image? (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $ARGO_HOME/builder/build.sh
fi

read -p "Build graalvisor? (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $ARGO_HOME/graalvisor/build.sh $GRAALVISOR_BUILD_MODE
fi

read -p "Build graalvisor test (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $ARGO_HOME/benchmarks/src/java/gv-hello-world/build_script.sh build_ni_sharedlibrary
    echo "Now you can try running a graalvisor hello world by running:"
    echo "> export WORK_DIR=$ARGO_HOME/tmp"
    echo "> sudo -E $ARGO_HOME/benchmarks/scripts/benchmark-graalvisor.sh svm gv_java_hw test 1"
fi

bash $ARGO_HOME/images/build.sh
bash $ARGO_HOME/benchmarks/scripts/build_benchmarks.sh
