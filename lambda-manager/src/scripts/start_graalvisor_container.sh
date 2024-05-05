#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

LAMBDA_ID=$1
if [ -z "$LAMBDA_ID" ]; then
  echo "Lambda id is not present."
  exit 1
fi

LAMBDA_NAME=$2
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

# Tags to set up lambda_port, lambda_timestamp and LD_LIBRARY_PATH.
TAGS=( "${@:3}" )
TAGS=( "${TAGS[@]/#/'-e '}" )

LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_GRAALVISOR
mkdir "$LAMBDA_HOME" &> /dev/null

cd "$LAMBDA_HOME"

docker run --privileged --rm --name="$LAMBDA_NAME" \
	${TAGS[@]} \
	--network host \
	graalvisor:latest &  # TODO: should we make the image configurable somehow?

echo "$!" > "$LAMBDA_HOME"/lambda.pid

wait
