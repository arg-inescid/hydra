#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source $DIR/build_shared.sh

DISK=$DIR/disk

GRAALVISOR_BINARY=$DIR/../../graalvisor/build/native-image/polyglot-proxy

# Prepare directory used to setup the filesystem.
rm -rf $DISK &> /dev/null
mkdir -p $DISK

# Copy files necessary to execute non-java functions.
copy_polyglot_deps

# Copy graalvisor and init.
cp $GRAALVISOR_BINARY $DISK/polyglot-proxy

# Build docker.
docker build --tag=graalvisor $DIR

# Remove directory used to create the image.
rm -r $DISK
