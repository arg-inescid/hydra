#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

function prepare_vmm_lambda_directory() {
  BUILD_OUTPUT_HOME=$1
  LAMBDA_HOME=$2
  mkdir "$LAMBDA_HOME" &> /dev/null
  cp -r "$BUILD_OUTPUT_HOME"/* "$LAMBDA_HOME"
}

function prepare_cruntime_lambda_directory() {
  LAMBDA_HOME=$1
  mkdir "$LAMBDA_HOME" &> /dev/null
}
