#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

function prepare_cruntime_lambda_directory() {
  LAMBDA_HOME=$1
  mkdir "$LAMBDA_HOME" &> /dev/null
}
