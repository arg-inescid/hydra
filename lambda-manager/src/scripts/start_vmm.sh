#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/export_lambda_arguments.sh
source "$DIR"/prepare_lambda_directories.sh

export_lambda_arguments "${@:1:9}"
# shellcheck disable=SC2153
FUNCTION_HOME=$CODEBASE_HOME/"$FUNCTION_NAME"
BUILD_OUTPUT_HOME="$FUNCTION_HOME"/build_vmm
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_NATIVE_IMAGE
prepare_vmm_lambda_directory "$BUILD_OUTPUT_HOME" "$LAMBDA_HOME"

bash "$LAMBDA_HOME"/"${FUNCTION_NAME}"_unikernel.sh --memory "$LAMBDA_MEMORY" --ip "$LAMBDA_IP" --tap "$LAMBDA_TAP" \
  --gateway "$LAMBDA_GATEWAY" --mask "$LAMBDA_MASK" "$LAMBDA_CONSOLE" --no-karg-patch "${@:9}"
# TODO - write pid and ip to disk (see start_hotspot.sh)
