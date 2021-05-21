#!/bin/bash

function export_lambda_arguments() {

  export FUNCTION_NAME="$1"
  if [ -z "$FUNCTION_NAME" ]; then
    echo "Function name is not present."
    exit 1
  fi

  export LAMBDA_ID="$2"
  if [ -z "$LAMBDA_ID" ]; then
    echo "Lambda id is not present."
    exit 1
  fi

  export LAMBDA_MEMORY=$3
  if [ -z "$LAMBDA_MEMORY" ]; then
    echo "Lambda memory is not present."
    exit 1
  fi

  export LAMBDA_IP=$4
  if [ -z "$LAMBDA_IP" ]; then
    echo "Lambda ip is not present."
    exit 1
  fi

  export LAMBDA_TAP=$5
  if [ -z "$LAMBDA_TAP" ]; then
    echo "Lambda tap is not present."
    exit 1
  fi

  export LAMBDA_GATEWAY=$6
  if [ -z "$LAMBDA_GATEWAY" ]; then
    echo "Lambda gateway is not present."
    exit 1
  fi

  export LAMBDA_MASK=$7
  if [ -z "$LAMBDA_MASK" ]; then
    echo "Lambda mask is not present."
    exit 1
  fi

  export LAMBDA_CONSOLE=$8
}
