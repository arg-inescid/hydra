#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

if [ -z "$GRAALOS_HOME" ]
then
    echo "Please set GRAALOS_OME first. It should point to directory containing an unzipped version of GraalOS."
    exit 1
fi

LAMBDA_NAME=$1
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

LAMBDA_HOME="$CODEBASE_HOME"/"$LAMBDA_NAME"
rm -rf "$LAMBDA_HOME"
mkdir "$LAMBDA_HOME" # TODO - make this the ephemeral dir?
mkdir "$LAMBDA_HOME"/tmp

"$GRAALOS_HOME"/build/graalhost/graalhost \
  --hub \
  --webserver \
  --retry_ports \
  --log_level=off \
  --command_uds_path="$LAMBDA_HOME"/lambda.uds \
  --seccomp 2 \
  --write_pid "$LAMBDA_HOME"/lambda.pid \
  --ephemeral_dir="$LAMBDA_HOME"/tmp \
  --ephemeral_dir_cleanup=never
