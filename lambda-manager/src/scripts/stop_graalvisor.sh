#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/prepare_lambda_directories.sh

VMM_TAP_NAME=$1
if [ -z "$VMM_TAP_NAME" ]; then
  echo "Lambda tap is not present."
  exit 1
fi

LAMBDA_PID=$(sudo fuser /tmp/$VMM_TAP_NAME.socket 2>&1 | grep /tmp/$VMM_TAP_NAME.socket | awk '{print $2}')
sudo kill $LAMBDA_PID
