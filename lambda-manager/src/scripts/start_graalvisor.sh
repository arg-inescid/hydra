#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/export_lambda_arguments.sh
source "$DIR"/prepare_lambda_directories.sh

export_lambda_arguments "${@:1:9}"
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_GRAALVISOR
prepare_vmm_lambda_directory "$GRAALVISOR_HOME" "$LAMBDA_HOME"

cd "$LAMBDA_HOME"
bash "$LAMBDA_HOME"/polyglot-proxy_unikernel.sh \
	--memory "$LAMBDA_MEMORY" \
	--ip "$LAMBDA_IP" \
	--tap "$LAMBDA_TAP" \
	--gateway "$LAMBDA_GATEWAY" \
	--mask "$LAMBDA_MASK" \
	"$LAMBDA_CONSOLE" \
	--no-karg-patch \
	"${@:9}"
