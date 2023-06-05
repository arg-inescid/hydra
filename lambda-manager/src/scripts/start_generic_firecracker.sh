#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/export_lambda_arguments.sh

export_lambda_arguments "${@:1:9}"
LAMBDA_NAME=$9
VM_IMAGE=${10}

LAMBDA_HOME="$CODEBASE_HOME"/"$LAMBDA_NAME"

mkdir "$LAMBDA_HOME" &> /dev/null
cp "$MANAGER_HOME"/../images/$VM_IMAGE/$VM_IMAGE.img "$LAMBDA_HOME"

cd "$LAMBDA_HOME"
bash $NIUK_HOME/run_niuk.sh \
	--vmm firecracker \
	--disk "$LAMBDA_HOME"/$VM_IMAGE.img \
	--kernel "$RES_HOME"/hello-vmlinux.bin \
	--memory "$LAMBDA_MEMORY" \
	--ip "$LAMBDA_IP" \
	--tap "$LAMBDA_TAP" \
	--gateway "$LAMBDA_GATEWAY" \
	--mask "$LAMBDA_MASK" \
	"$LAMBDA_CONSOLE" \
	"${@:9}"
