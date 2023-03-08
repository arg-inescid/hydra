#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source $DIR/build_shared.sh

DISK=$DIR/disk

ghome=$1
gvbinary=$2
gvdisk=$3

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters."
    echo "Syntax: build_niuk.sh <graalvm home> <input graalvisor native-image binary path> <output graalvisor vm disk path>"
    exit 1
fi

# Build out init program.
gcc -c $DIR/init.c -o $DIR/init.o
gcc -o $DIR/init $DIR/init.o

# Prepare directory used to setup the filesystem.
rm -rf $DISK &> /dev/null
mkdir -p $DISK

# If we don't have a base filesystem, create one.
if [ ! -f $DIR/debian.ext4 ];
then
    # Create an empty 2gb partition.
    dd if=/dev/zero of=$DIR/debian.ext4 bs=1M count=2048
    mkfs.ext4 $DIR/debian.ext4
    # Mount it, add permissions
    sudo mount $DIR/debian.ext4 $DISK
    sudo chown -R $USER:$USER $DIR
    # Use a debian docker to copy the entire image to the mounted dir.
    docker export $(docker create debian) | tar -xC $DISK
    # Revert permissions and unmount.
    sudo chown -R root:root $DISK
    sudo umount $DISK
fi

# Copy the base filesystem into a new one, mount, and setup permissions.
cp $DIR/debian.ext4 $gvdisk
sudo mount $gvdisk $DISK
sudo chown -R $USER:$USER $DISK

# Graalpython's Pillow package.
copy_deps ~/.cache/Python-Eggs/Pillow-6.2.0-py3.8-linux-x86_64.egg-tmp/PIL/_imaging.graalpython-38-native-x86_64-linux.so

# TODO - eventually we also need to get rid of this. Benchmarks should include their own deps.
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

# Unmount image and remove directory used for the mount.
sudo chown -R root:root $DISK
sudo umount $DISK
rm -r $DISK
