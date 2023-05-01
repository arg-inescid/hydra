#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh


function stop_vm {
  LAMBDA_PID=$(sudo fuser /tmp/$VMM_TAP_NAME.socket 2>&1 | grep /tmp/$VMM_TAP_NAME.socket | awk '{print $2}')
  sudo kill $LAMBDA_PID
}

function stop_container {
  docker container kill $LAMBDA_NAME
}


TYPE=$1
if [ -z "$TYPE" ]; then
  echo "Lambda type is not present."
  exit 1
fi


if [[ "$TYPE" = "vm" ]]; then
  # Lambda type is VM.
  VMM_TAP_NAME=$2
  if [ -z "$VMM_TAP_NAME" ]; then
    echo "Lambda tap is not present."
    exit 1
  fi
  stop_vm
else
  # Lambda type is container.
  LAMBDA_NAME=$2
  if [ -z "$LAMBDA_NAME" ]; then
    echo "Lambda name is not present."
    exit 1
  fi
  stop_container
fi
