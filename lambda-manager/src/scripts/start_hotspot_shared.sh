#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
source "$DIR"/env.sh

QEMU_JVM_DIR=$MANAGER_HOME/src/scripts/qemu-jvm

function prepare_hotspot_lambda_directory() {
  LAMBDA_HOME=$1
  if [[ ! -d $LAMBDA_HOME ]]; then
    mkdir "$LAMBDA_HOME"
    cp "$QEMU_JVM_DIR"/debian_vm_unikernel.sh "$LAMBDA_HOME"
    cp -r "$QEMU_JVM_DIR"/shared "$LAMBDA_HOME"
    cp "$QEMU_JVM_DIR"/{id_rsa,id_rsa.pub} "$LAMBDA_HOME"
    cp "$QEMU_JVM_DIR"/stretch.img "$LAMBDA_HOME"
  fi
}
