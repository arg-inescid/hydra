#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

LAMBDA_HOME=$1
if [ -z "$LAMBDA_HOME" ]; then
  echo "Lambda home is not present."
  exit 1
fi

curl --unix-socket "$LAMBDA_HOME"/lambda.uds localhost/exit