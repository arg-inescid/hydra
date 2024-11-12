#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

LAMBDA_MEMORY=$1
if [ -z "$LAMBDA_MEMORY" ]; then
  echo "Lambda memory is not present."
  exit 1
fi

LAMBDA_CPU_QUOTA=$2
if [ -z "$LAMBDA_CPU_QUOTA" ]; then
  echo "Lambda CPU quota is not present."
  exit 1
fi

LAMBDA_PORT=$3
if [ -z "$LAMBDA_PORT" ]; then
  echo "Lambda port is not present."
  exit 1
fi

LAMBDA_NAME=$4
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

CONTAINER_IMAGE=$5
if [ -z "$CONTAINER_IMAGE" ]; then
  echo "Container image is not present."
  exit 1
fi

# To set up such tags as lambda_port, lambda_timestamp, and LD_LIBRARY_PATH.
TAGS=( "${@:6}" )
TAGS=( "${TAGS[@]/#/'-e '}" )
TAGS+=( "-e app_dir=/codebase/" )

# The default value. Source: https://docs.docker.com/config/containers/resource_constraints/#configure-the-default-cfs-scheduler
CGROUPS_CPU_PERIOD="100000"
# The default value for Graalvisor and OpenWhisk.
PROXY_PORT="8080"

LAMBDA_HOME="$CODEBASE_HOME"/"$LAMBDA_NAME"
mkdir "$LAMBDA_HOME" &> /dev/null

if [[ "$LAMBDA_NAME" == *"AGENT"* ]]; then
  mkdir "$LAMBDA_HOME"/config &> /dev/null
  # TODO: copy config from the previous agent run and provide it to this agent.
fi

cd "$LAMBDA_HOME"

docker run --privileged --rm --name="$LAMBDA_NAME" \
  ${TAGS[@]} \
  --privileged \
  -p "$LAMBDA_PORT":"$PROXY_PORT" \
  -v "$ARGO_HOME"/lambda-manager/codebase:/codebase \
  --memory "$LAMBDA_MEMORY" \
  --cpu-period="$CGROUPS_CPU_PERIOD" \
  --cpu-quota="$LAMBDA_CPU_QUOTA" \
  "$CONTAINER_IMAGE" &

# Writes PID of the init process.
# docker inspect --format '{{ .State.Pid }}' "$LAMBDA_NAME" > "$LAMBDA_HOME"/lambda.pid

wait
