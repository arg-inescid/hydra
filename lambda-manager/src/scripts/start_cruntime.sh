#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

IPprefix_by_netmask() {
    bits=0
    for octet in $(echo $1| sed 's/\./ /g'); do
         binbits=$(echo "obase=2; ibase=10; ${octet}"| bc | sed 's/0//g')
         let bits+=${#binbits}
    done
    echo "${bits}"
}

source "$DIR"/environment.sh
source "$DIR"/export_lambda_arguments.sh
source "$DIR"/prepare_lambda_directories.sh

export_lambda_arguments "${@:1:9}"
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_CUSTOM
RUNTIME=$9
VMID=${10}
prepare_cruntime_lambda_directory "$LAMBDA_HOME"

LAMBDA_MAC=`printf 'DE:AD:BE:EF:%02X:%02X\n' $((RANDOM%256)) $((RANDOM%256))`

# TODO - select memory for the VM.
sudo echo "$VMID"    > "$LAMBDA_HOME"/lambda.id
sudo echo "$RUNTIME" > "$LAMBDA_HOME"/lambda.runtime
sudo $CRUNTIME_HOME/start-vm -ip $LAMBDA_IP/$(IPprefix_by_netmask $LAMBDA_MASK) -gw $LAMBDA_GATEWAY -tap $LAMBDA_TAP -mac $LAMBDA_MAC -id $VMID -img $RUNTIME
