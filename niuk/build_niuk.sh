#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

DISK=$DIR/disk

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters."
    echo "Syntax: build_niuk.sh <input graalvisor native-image binary path> <output graalvisor vm disk path>"
    exit 1
fi

gvbinary=$1
gvdisk=$2

# Build out init program.
gcc -c $DIR/init.c -o $DIR/init.o
gcc -o $DIR/init $DIR/init.o

# Prepare file system.
rm -r $DISK &> /dev/null
mkdir -p $DISK/proc
mkdir -p $DISK/dev
mkdir -p $DISK/lib64
mkdir -p $DISK/lib/x86_64-linux-gnu 

# Copy necessary libraries.
cp /lib64/ld-linux-x86-64.so.2           $DISK/lib64/ld-linux-x86-64.so.2
cp /lib/x86_64-linux-gnu/libpthread.so.0 $DISK/lib/x86_64-linux-gnu/libpthread.so.0
cp /lib/x86_64-linux-gnu/libdl.so.2      $DISK/lib/x86_64-linux-gnu/libdl.so.2
cp /lib/x86_64-linux-gnu/libc.so.6       $DISK/lib/x86_64-linux-gnu/libc.so.6
cp /lib/x86_64-linux-gnu/libz.so.1       $DISK/lib/x86_64-linux-gnu/libz.so.1

# Copy graalvisor and init.
cp $gvbinary $DIR/init $DISK

# Create the file system.
virt-make-fs --type=ext4 --format=raw --size=512M $DISK $gvdisk
