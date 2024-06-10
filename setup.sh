#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function install_graalvm {
    wget https://download.oracle.com/graalvm/17/archive/graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
    tar -vzxf graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
    mv graalvm-jdk-17.0.7+8.1 $ARGO_HOME/resources
    rm graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
}

function install_firecracker {
    wget https://github.com/firecracker-microvm/firecracker/releases/download/v1.1.0/firecracker-v1.1.0-x86_64.tgz
    tar -vzxf firecracker-v1.1.0-x86_64.tgz
    mv release-v1.1.0-x86_64 $ARGO_HOME/resources/firecracker-v1.1.0-x86_64
    ln -s $ARGO_HOME/resources/firecracker-v1.1.0-x86_64/firecracker-v1.1.0-x86_64 $ARGO_HOME/resources/firecracker-v1.1.0-x86_64/firecracker
    rm firecracker-v1.1.0-x86_64.tgz
}

function install_musl {
    # Option 1: Download a musl build.
    function download_musl {
        wget https://more.musl.cc/10/x86_64-linux-musl/x86_64-linux-musl-native.tgz
        tar -vzxf x86_64-linux-musl-native.tgz
        mv x86_64-linux-musl-native $ARGO_HOME/resources/x86_64-linux-musl-native
        rm x86_64-linux-musl-native.tgz
    }

    # Option 2: Use a local musl build.
    function clone_musl {
        cp -r /home/rbruno/git/faastion/musl-cross-make/output $ARGO_HOME/resources/x86_64-linux-musl-native
    }

    download_musl
    #clone_musl

    wget https://zlib.net/current/zlib.tar.gz
    tar -vzxf zlib.tar.gz
    rm -rf $ARGO_HOME/resources/zlib-1.3.1 &> /dev/null
    mv zlib-1.3.1 $ARGO_HOME/resources/
    rm zlib.tar.gz

    export PATH=$ARGO_HOME/resources/x86_64-linux-musl-native/bin:$PATH
    echo "export PATH=\$ARGO_HOME/resources/x86_64-linux-musl-native/bin:\$PATH" >> $ARGO_HOME/env.sh
    echo "set -gx PATH \$ARGO_HOME/resources/x86_64-linux-musl-native/bin \$PATH" >> $ARGO_HOME/env.fish
    echo "Added musl to path in $ARGO_HOME/env.sh"

    CC=$ARGO_HOME/resources/x86_64-linux-musl-native/bin/x86_64-linux-musl-cc
    cd $ARGO_HOME/resources/zlib-1.3.1
    ./configure --prefix=$ARGO_HOME/resources/x86_64-linux-musl-native --static
    make
    make install
    cd - &> /dev/null
}

export ARGO_HOME=$(DIR)
export WORK_DIR=$ARGO_HOME/tmp
export JAVA_HOME=$ARGO_HOME/resources/graalvm-jdk-17.0.7+8.1

# Preparing environment.
mv $ARGO_HOME/env.sh   $ARGO_HOME/env.sh.back   &> /dev/null
mv $ARGO_HOME/env.fish $ARGO_HOME/env.fish.back &> /dev/null
echo "export  ARGO_HOME=$ARGO_HOME"     >> $ARGO_HOME/env.sh
echo "set -gx ARGO_HOME $ARGO_HOME"     >> $ARGO_HOME/env.fish
echo "export  WORK_DIR=\$ARGO_HOME/tmp"  >> $ARGO_HOME/env.sh
echo "set -gx WORK_DIR \$ARGO_HOME/tmp"  >> $ARGO_HOME/env.fish
echo "export  PATH=\$ARGO_HOME/resources/firecracker-v1.1.0-x86_64:\$PATH"   >> $ARGO_HOME/env.sh
echo "set -gx PATH \$ARGO_HOME/resources/firecracker-v1.1.0-x86_64 \$PATH"   >> $ARGO_HOME/env.fish
echo "export  JAVA_HOME=\$ARGO_HOME/resources/graalvm-jdk-17.0.7+8.1"        >> $ARGO_HOME/env.sh
echo "set -gx JAVA_HOME \$ARGO_HOME/resources/graalvm-jdk-17.0.7+8.1 \$PATH" >> $ARGO_HOME/env.fish

if [ ! -f $JAVA_HOME/bin/java ];
then
    echo "JVM not found. Installing..."
    install_graalvm
fi

if [ ! -f $ARGO_HOME/resources/firecracker-v1.1.0-x86_64/firecracker ];
then
    echo "Firecracker not found. Installing..."
    install_firecracker
fi

if [ ! -e $ARGO_HOME/resources/x86_64-linux-musl-native/ ];
then
    read -p "Musl not found. Installing musl in $ARGO_HOME/resources? (y or Y, everything else as no)? " -n 1 -r
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

if [ ! -e $ARGO_HOME/benchmarks/.git ];
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
