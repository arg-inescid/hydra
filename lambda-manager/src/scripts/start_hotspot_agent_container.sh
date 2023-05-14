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

# Tags to set up lambda_timestamp, lambda_entry_point and lambda_port.
ARGS=( "${@:5}" )
ARGS=( "${ARGS[@]/#/'-e '}" )

FUNCTION_HOME=$CODEBASE_HOME/"$FUNCTION_NAME"
FUNCTION_CODE=$FUNCTION_HOME/$FUNCTION_NAME
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_HOTSPOT_W_AGENT
mkdir "$LAMBDA_HOME" &> /dev/null
mkdir "$LAMBDA_HOME"/shared &> /dev/null

echo $(date) >> "$LAMBDA_HOME"/shared/run.log

cp "$PROXY_JAR" "$FUNCTION_CODE" "$LAMBDA_HOME"/shared
cp "$MANAGER_HOME"/src/main/resources/caller-filter-config.json "$LAMBDA_HOME"/shared

PREV_AGENT_CONFIG=$FUNCTION_HOME/pid_"$PREV_AGENT_PID"_hotspot_w_agent/shared/config
if [[ -d "$PREV_AGENT_CONFIG" ]]; then
  cp -r "$PREV_AGENT_CONFIG" "$LAMBDA_HOME"/shared/config
else
  mkdir "$LAMBDA_HOME"/shared/config
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/jni-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/predefined-classes-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/proxy-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/reflect-config.json
  printf "{\n}\n" > "$LAMBDA_HOME"/shared/config/resource-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/serialization-config.json
fi

docker run --rm --name="$LAMBDA_NAME" \
  -v "$JAVA_HOME":/jvm \
  -v "$LAMBDA_HOME"/shared:/shared \
  -w /shared \
	${ARGS[@]} \
	--network host \
	argo-builder \
  /jvm/bin/java -Djava.library.path=/jvm/lib -agentlib:native-image-agent=config-merge-dir=config,caller-filter-file=caller-filter-config.json -cp graalvisor-1.0-all.jar:$FUNCTION_NAME org.graalvm.argo.graalvisor.Main &

echo "$!" > "$LAMBDA_HOME"/lambda.pid

wait
