#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/export_lambda_arguments.sh
source "$DIR"/prepare_lambda_directories.sh

export_lambda_arguments "${@:1:9}"
# shellcheck disable=SC2153
FUNCTION_HOME=$CODEBASE_HOME/"$FUNCTION_NAME"
FUNCTION_JAR=$FUNCTION_HOME/$FUNCTION_NAME.jar
LAMBDA_HOME=$FUNCTION_HOME/pid_"$LAMBDA_ID"_hotspot
prepare_hotspot_lambda_directory "$LAMBDA_HOME"

cp "$FUNCTION_JAR" "$LAMBDA_HOME"/shared
echo "\$JAVA_HOME/bin/java -jar $FUNCTION_NAME.jar ${*:9}" >"$LAMBDA_HOME"/shared/run.sh

"$LAMBDA_HOME"/debian_vm_unikernel.sh --memory "$LAMBDA_MEMORY" --gateway "$LAMBDA_GATEWAY" --ip "$LAMBDA_IP" \
  --mask "$LAMBDA_MASK" --kernel "$KERNEL_PATH" --img "$LAMBDA_HOME"/stretch.img --shared "$LAMBDA_HOME"/shared \
  --tap "$LAMBDA_TAP" "$LAMBDA_CONSOLE"
