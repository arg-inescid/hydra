#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

# shellcheck disable=SC2153

FUNCTION_NAME="$1"
if [ -z "$FUNCTION_NAME" ]; then
  echo "Function name is not present."
  exit 1
fi

LAMBDA_ID="$2"
if [ -z "$LAMBDA_ID" ]; then
  echo "Lambda id is not present."
  exit 1
fi

LAMBDA_NAME="$3"
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

PREV_AGENT_PID="$4"
if [ -z "$PREV_AGENT_PID" ]; then
  echo "Previous agent pid is not present."
  exit 1
fi

if [ ! -f "$PROXY_JAR" ]; then
  echo "Proxy JAR - $PROXY_JAR - not found!"
  exit 1
fi

# Tags to set up lambda_timestamp and lambda_port.
ARGS=( "${@:5}" )
ARGS=( "${ARGS[@]/#/'-e '}" )

FUNCTION_HOME=$CODEBASE_HOME/"$FUNCTION_NAME"
FUNCTION_CODE=$FUNCTION_HOME/$FUNCTION_NAME
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_HOTSPOT_W_AGENT
mkdir "$LAMBDA_HOME" &> /dev/null
mkdir "$LAMBDA_HOME"/config &> /dev/null

# TODO: copy config from the previous agent run and provide it to this agent.

docker run --rm --name="$LAMBDA_NAME" \
	${ARGS[@]} \
	--network host \
	argo-hotspot-agent &

echo "$!" > "$LAMBDA_HOME"/lambda.pid

wait
