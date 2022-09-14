#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

DISK=$DIR/disk

ghome=$1
gvbinary=$2
gvdisk=$3

function copy_deps {
    # Use ldd to look for dependencies.
    for dep in $(ldd $1 | grep "=" | grep -v "not found" | awk '{ print $3 }')
    do
        if [ ! -f "$DISK/$dep" ]; then
            echo "Copying $dep as a dependency of $1"
            cp $dep $DISK/$(dirname $dep)
            # Dependencies might have dependencies as well.
            copy_deps $dep
        fi
    done
}

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters."
    echo "Syntax: build_niuk.sh <graalvm home> <input graalvisor native-image binary path> <output graalvisor vm disk path>"
    exit 1
fi

# Build out init program.
gcc -c $DIR/init.c -o $DIR/init.o
gcc -o $DIR/init $DIR/init.o

# Prepare file system.
rm -rf $DISK &> /dev/null
mkdir -p $DISK/proc
mkdir -p $DISK/tmp
mkdir -p $DISK/dev
mkdir -p $DISK/lib64
mkdir -p $DISK/lib/x86_64-linux-gnu 
mkdir -p $DISK/usr/lib/x86_64-linux-gnu

# Copy the dynamic linker.
cp /lib64/ld-linux-x86-64.so.2 $DISK/lib64/ld-linux-x86-64.so.2

# Copy necessary libraries (check with ldd ../lambda-proxy/build/native-image/polyglot-proxy).
copy_deps $gvbinary

# Graalpython's Pillow package.
copy_deps ~/.cache/Python-Eggs/Pillow-6.2.0-py3.8-linux-x86_64.egg-tmp/PIL/_imaging.graalpython-38-native-x86_64-linux.so

# JVips.jar
unzip -o -q ../../graalvm-argo-benchmarks/demos/demo-ni-jni/JVips.jar -d /tmp/jvips
for dep in /tmp/jvips/*.so; do copy_deps $dep; done

# Copy graalvm languages and python's virtual environment.
mkdir -p $DISK/jvm/languages
cp -r $ghome/languages/{python,js,llvm} $DISK/jvm/languages
cp -r $ghome/graalvisor-python-venv $DISK/jvm

# Copy graalvisor and init.
cp $gvbinary $DISK/polyglot-proxy
cp $DIR/init $DISK

# Create the file system.
virt-make-fs --type=ext4 --format=raw --size=2048M $DISK $gvdisk
