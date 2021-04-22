#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
source "$DIR"/env.sh
source "$DIR"/start_hotspot_shared.sh

FUNCTION_NAME=$1
if [ -z "$FUNCTION_NAME" ]; then
  echo "Function name is not present."
  exit 1
fi

LAMBDA_ID=$2
if [ -z "$LAMBDA_ID" ]; then
  echo "Lambda id is not present."
  exit 1
fi

LAMBDA_MEMORY=$3
if [ -z "$LAMBDA_MEMORY" ]; then
  echo "Lambda memory is not present."
  exit 1
fi

LAMBDA_IP=$4
if [ -z "$LAMBDA_IP" ]; then
  echo "Lambda ip is not present."
  exit 1
fi

LAMBDA_TAP=$5
if [ -z "$LAMBDA_TAP" ]; then
  echo "Lambda tap is not present."
  exit 1
fi

LAMBDA_GATEWAY=$6
if [ -z "$LAMBDA_GATEWAY" ]; then
  echo "Lambda gateway is not present."
  exit 1
fi

LAMBDA_MASK=$7
if [ -z "$LAMBDA_MASK" ]; then
  echo "Lambda mask is not present."
  exit 1
fi

LAMBDA_CONSOLE=$8

FUNCTION_HOME=$MANAGER_HOME/src/codebase/$FUNCTION_NAME
FUNCTION_JAR=$FUNCTION_HOME/$FUNCTION_NAME.jar
LAMBDA_HOME=$FUNCTION_HOME/start-hotspot-id-$LAMBDA_ID

prepare_hotspot_lambda_directory "$LAMBDA_HOME"
cp "$FUNCTION_JAR" "$LAMBDA_HOME"/shared
echo "\$JAVA_HOME/bin/java -jar $FUNCTION_NAME.jar ${*:9}" >"$LAMBDA_HOME"/shared/run.sh

"$LAMBDA_HOME"/debian-vm_unikernel.sh --memory "$LAMBDA_MEMORY" --gateway "$LAMBDA_GATEWAY" --ip "$LAMBDA_IP" \
  --mask "$LAMBDA_MASK" --kernel "$KERNEL_PATH" --img "$LAMBDA_HOME"/stretch.img --shared "$LAMBDA_HOME"/shared \
  --tap "$LAMBDA_TAP" "$LAMBDA_CONSOLE"
