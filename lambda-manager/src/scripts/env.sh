#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

RES_HOME=$DIR/../../../res
MANAGER_HOME=$DIR/../../../lambda-manager
JAVA_HOME=$RES_HOME/graalvm-d219e07057-java11-21.1.0-dev
KERNEL_PATH=$RES_HOME/vmlinux-4.14.35-1902.6.6.1.el7.container
VIRTUALIZE_PATH=$RES_HOME/virtualize.json
QEMU=$HOME/git/qemu/build/x86_64-softmmu/qemu-system-x86_64 # TODO - remove
