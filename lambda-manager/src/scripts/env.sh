#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

KERNEL_PATH=$DIR/../../../res/vmlinux-4.14.35-1902.6.6.1.el7.container
QEMU=$HOME/git/qemu/build/x86_64-softmmu/qemu-system-x86_64
