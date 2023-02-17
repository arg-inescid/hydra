#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/export_lambda_arguments.sh
source "$DIR"/prepare_lambda_directories.sh

export_lambda_arguments "${@:1:9}"
# shellcheck disable=SC2153
FUNCTION_HOME=$CODEBASE_HOME/"$FUNCTION_NAME"
FUNCTION_CODE=$FUNCTION_HOME/$FUNCTION_NAME
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_HOTSPOT
prepare_hotspot_lambda_directory "$LAMBDA_HOME"

TIMESTAMP_TAG="${9}"
if [ -z "$TIMESTAMP_TAG" ]; then
  echo "Timestamp tag is not present."
  exit 1
fi

ENTRY_POINT_TAG="${10}"
if [ -z "$ENTRY_POINT_TAG" ]; then
  echo "Entry point tag is not present."
  exit 1
fi

PORT_TAG="${11}"
if [ -z "$PORT_TAG" ]; then
  echo "Port tag is not present."
  exit 1
fi

if [ ! -f "$PROXY_JAR" ]; then
  echo "Proxy JAR - $PROXY_JAR - not found!"
  exit 1
fi

cp "$PROXY_JAR" "$FUNCTION_CODE" "$LAMBDA_HOME"/shared

echo "export $PORT_TAG" >"$LAMBDA_HOME"/shared/run.sh
echo "export $ENTRY_POINT_TAG" >>"$LAMBDA_HOME"/shared/run.sh
echo "export $TIMESTAMP_TAG" >>"$LAMBDA_HOME"/shared/run.sh

echo "\$JAVA_HOME/bin/java -cp graalvisor-1.0-all.jar:$FUNCTION_NAME org.graalvm.argo.graalvisor.Main" >>"$LAMBDA_HOME"/shared/run.sh

"$LAMBDA_HOME"/debian_vm_unikernel.sh --memory "$LAMBDA_MEMORY" --gateway "$LAMBDA_GATEWAY" --ip "$LAMBDA_IP" \
  --mask "$LAMBDA_MASK" --kernel "$KERNEL_PATH" --img "$LAMBDA_HOME"/stretch.img --shared "$LAMBDA_HOME"/shared \
  --tap "$LAMBDA_TAP" "$LAMBDA_CONSOLE" ${*:9}
