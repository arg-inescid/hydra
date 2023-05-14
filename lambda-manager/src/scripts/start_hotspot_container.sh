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

if [ ! -f "$PROXY_JAR" ]; then
  echo "Proxy JAR - $PROXY_JAR - not found!"
  exit 1
fi

# Tags to set up lambda_timestamp, lambda_entry_point and lambda_port.
ARGS=( "${@:4}" )
ARGS=( "${ARGS[@]/#/'-e '}" )

FUNCTION_HOME=$CODEBASE_HOME/"$FUNCTION_NAME"
FUNCTION_CODE=$FUNCTION_HOME/$FUNCTION_NAME
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_HOTSPOT
mkdir "$LAMBDA_HOME" &> /dev/null
mkdir "$LAMBDA_HOME"/shared &> /dev/null

echo $(date) >> "$LAMBDA_HOME"/shared/run.log

cp "$PROXY_JAR" "$FUNCTION_CODE" "$LAMBDA_HOME"/shared

docker run --rm --name="$LAMBDA_NAME" \
  -v "$JAVA_HOME":/jvm \
  -v "$LAMBDA_HOME"/shared:/shared \
  -w /shared \
	${ARGS[@]} \
	--network host \
	argo-builder \
  /jvm/bin/java -cp graalvisor-1.0-all.jar:$FUNCTION_NAME org.graalvm.argo.graalvisor.Main &

echo "$!" > "$LAMBDA_HOME"/lambda.pid

wait
