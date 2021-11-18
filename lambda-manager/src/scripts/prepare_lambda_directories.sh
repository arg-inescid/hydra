#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

QEMU_JVM_DIR=$MANAGER_HOME/src/scripts/qemu-jvm

function prepare_hotspot_lambda_directory() {
  LAMBDA_HOME=$1
  mkdir "$LAMBDA_HOME" &> /dev/null
  cp -r "$QEMU_JVM_DIR"/{id_rsa,id_rsa.pub,debian_vm_unikernel.sh,stretch.img,shared} "$LAMBDA_HOME"
  chmod 400 "$LAMBDA_HOME"/id_rsa
}

function prepare_vmm_lambda_directory() {
  BUILD_OUTPUT_HOME=$1
  LAMBDA_HOME=$2
  mkdir "$LAMBDA_HOME" &> /dev/null
  cp -r "$BUILD_OUTPUT_HOME"/* "$LAMBDA_HOME"
}
