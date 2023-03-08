#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source $DIR/build_shared.sh

DISK=$DIR/disk

ghome=$1
gvbinary=$2

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters."
    echo "Syntax: build_niuk.sh <graalvm home> <input graalvisor native-image binary path>"
    exit 1
fi

# Prepare directory used to setup the filesystem.
rm -rf $DISK &> /dev/null
mkdir -p $DISK

# Graalpython's Pillow package.
copy_deps ~/.cache/Python-Eggs/Pillow-6.2.0-py3.8-linux-x86_64.egg-tmp/PIL/_imaging.graalpython-38-native-x86_64-linux.so

if [ -z "$BENCHMARKS_HOME" ]
then
        echo "Warninig: BENCHMARKS_HOME is not set. Some benchmarks might now work due to missing dependencies."
else
        # JVips.jar
        unzip -o -q $BENCHMARKS_HOME/demos/ni-jni/JVips.jar -d /tmp/jvips
        for dep in /tmp/jvips/*.so; do copy_deps $dep; done
fi

# Copy graalvm languages and python's virtual environment.
mkdir -p $DISK/jvm/languages
cp -r $ghome/languages/{python,js,llvm} $DISK/jvm/languages
cp -r $ghome/graalvisor-python-venv $DISK/jvm

# Copy graalvisor and init.
cp $gvbinary $DISK/polyglot-proxy
cp $DIR/init $DISK

# Build docker.
docker build --tag=graalvisor $DIR

# Remove directory used to create the image.
rm -r $DISK
