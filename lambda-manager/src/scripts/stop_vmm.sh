#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/prepare_lambda_directories.sh

LAMBDA_HOME=$1
if [ -z "$LAMBDA_HOME" ]; then
  echo "Lambda home is not present."
  exit 1
fi

sudo pkill -TERM -P "$(cat "$LAMBDA_HOME"/lambda.pid)"
sudo kill -TERM "$(cat "$LAMBDA_HOME"/lambda.pid)"
