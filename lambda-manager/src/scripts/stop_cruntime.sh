#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/prepare_lambda_directories.sh

LAMBDA_HOME=$1
if [ -z "$LAMBDA_HOME" ]; then
  echo "Lambda home is not present."
  exit 1
fi

sudo $CRUNTIME_HOME/stop-vm -id $(cat $LAMBDA_HOME/lambda.id) &> "$LAMBDA_HOME"/stop_cruntime.log
