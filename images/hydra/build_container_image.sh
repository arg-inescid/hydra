#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source $DIR/build_shared.sh

DISK=$DIR/disk

HYDRA_HOME=$DIR/../../hydra
HYDRA_BINARY=$HYDRA_HOME/build/native-image/polyglot-proxy
HYDRA_SCRIPT=$HYDRA_HOME/hydra
HYDRA_ALLOCATOR=$HYDRA_HOME/src/main/c/svm-snapshot/deps/dlmalloc/hydralloc.so

# Prepare directory used to setup the filesystem.
rm -rf $DISK &> /dev/null
mkdir -p $DISK
mkdir -p $DISK/build/native-image
mkdir -p $DISK/src/main/c/svm-snapshot/deps/dlmalloc

# Copy files necessary to execute non-java functions.
copy_polyglot_deps

# Copy hydra and init.
cp $HYDRA_BINARY $DISK/build/native-image/polyglot-proxy
cp $HYDRA_SCRIPT $DISK/hydra
cp $HYDRA_ALLOCATOR $DISK/src/main/c/svm-snapshot/deps/dlmalloc/hydralloc.so

# Build docker.
docker build --tag=hydra $DIR

# Remove directory used to create the image.
rm -r $DISK
