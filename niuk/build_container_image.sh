#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source $DIR/build_shared.sh

DISK=$DIR/disk

ghome=$1
gvbinary=$2

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters."
    echo "Syntax: build_container_image.sh <graalvm home> <input graalvisor native-image binary path>"
    exit 1
fi

# Prepare directory used to setup the filesystem.
rm -rf $DISK &> /dev/null
mkdir -p $DISK

# TODO - eventually we also need to get rid of this. Benchmarks should include their own deps.
if [ -z "$BENCHMARKS_HOME" ]
then
        echo "Warninig: BENCHMARKS_HOME is not set. Some benchmarks might now work due to missing dependencies."
else
        # JVips.jar
        unzip -o -q $BENCHMARKS_HOME/demos/ni-jni/JVips.jar -d /tmp/jvips
        for dep in /tmp/jvips/*.so; do copy_deps $dep; done
fi

copy_polyglot_deps

# Copy graalvisor and init.
cp $gvbinary $DISK/polyglot-proxy
cp $DIR/init $DISK

# Build docker.
docker build --tag=graalvisor $DIR

# Remove directory used to create the image.
rm -r $DISK
