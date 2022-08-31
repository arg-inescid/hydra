#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

DISK=$DIR/disk

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters."
    echo "Syntax: build_niuk.sh <graalvm home> <input graalvisor native-image binary path> <output graalvisor vm disk path>"
    exit 1
fi

ghome=$1
gvbinary=$2
gvdisk=$3

# Build out init program.
gcc -c $DIR/init.c -o $DIR/init.o
gcc -o $DIR/init $DIR/init.o

# Prepare file system.
rm -rf $DISK &> /dev/null
mkdir -p $DISK/proc
mkdir -p $DISK/dev
mkdir -p $DISK/lib64
mkdir -p $DISK/lib/x86_64-linux-gnu 
mkdir -p $DISK/usr/lib/x86_64-linux-gnu

# Copy necessary libraries (check with ldd ../lambda-proxy/build/native-image/polyglot-proxy).
cp /lib64/ld-linux-x86-64.so.2               $DISK/lib64/ld-linux-x86-64.so.2
cp /lib/x86_64-linux-gnu/libpthread.so.0     $DISK/lib/x86_64-linux-gnu/libpthread.so.0
cp /lib/x86_64-linux-gnu/libdl.so.2          $DISK/lib/x86_64-linux-gnu/libdl.so.2
cp /lib/x86_64-linux-gnu/libc.so.6           $DISK/lib/x86_64-linux-gnu/libc.so.6
cp /lib/x86_64-linux-gnu/libz.so.1           $DISK/lib/x86_64-linux-gnu/libz.so.1
cp /lib/x86_64-linux-gnu/librt.so.1          $DISK/lib/x86_64-linux-gnu/librt.so.1
cp /usr/lib/x86_64-linux-gnu/libstdc++.so.6  $DISK/usr/lib/x86_64-linux-gnu/libstdc++.so.6
cp /usr/lib/x86_64-linux-gnu/libm.so.6       $DISK/usr/lib/x86_64-linux-gnu/libm.so.6
cp /usr/lib/x86_64-linux-gnu/libgcc_s.so.1   $DISK/usr/lib/x86_64-linux-gnu/libgcc_s.so.1
cp /usr/lib/x86_64-linux-gnu/libtiff.so.5    $DISK/usr/lib/x86_64-linux-gnu/libtiff.so.5
cp /usr/lib/x86_64-linux-gnu/libwebp.so.6    $DISK/usr/lib/x86_64-linux-gnu/libwebp.so.6
cp /usr/lib/x86_64-linux-gnu/libzstd.so.1    $DISK/usr/lib/x86_64-linux-gnu/libzstd.so.1
cp /usr/lib/x86_64-linux-gnu/libjbig.so.0    $DISK/usr/lib/x86_64-linux-gnu/libjbig.so.0
cp /usr/lib/x86_64-linux-gnu/libjpeg.so.62   $DISK/usr/lib/x86_64-linux-gnu/libjpeg.so.62
cp /usr/lib/x86_64-linux-gnu/libdeflate.so.0 $DISK/usr/lib/x86_64-linux-gnu/libdeflate.so.0

# Copy graalvm languages and python's virtual environment.
mkdir -p $DISK/jvm/languages
cp -r $ghome/languages/{python,js,llvm} $DISK/jvm/languages
cp -r $ghome/graalvisor-python-venv $DISK/jvm

# Copy graalvisor and init.
cp $gvbinary $DIR/init $DISK

# Create the file system.
virt-make-fs --type=ext4 --format=raw --size=2048M $DISK $gvdisk
