#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

# shellcheck disable=SC2153

LAMBDA_ID="$1"
if [ -z "$LAMBDA_ID" ]; then
  echo "Lambda id is not present."
  exit 1
fi

LAMBDA_NAME="$2"
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

# Tags to set up lambda_timestamp and lambda_port.
ARGS=( "${@:3}" )
ARGS=( "${ARGS[@]/#/'-e '}" )

LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_HOTSPOT
mkdir "$LAMBDA_HOME" &> /dev/null

docker run --rm --name="$LAMBDA_NAME" \
	${ARGS[@]} \
	--network host \
	argo-hotspot &

echo "$!" > "$LAMBDA_HOME"/lambda.pid

wait
