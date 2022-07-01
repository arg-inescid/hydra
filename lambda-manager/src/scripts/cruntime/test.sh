#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

ARGO_HOME=$(DIR)/../../../../
GW=194.210.228.242
SMASK=23

function test_vm {
	VMID=$1
	TAP=$2
	IP=$3

	# Create tap
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/create_taps.sh $TAP $IP

	# Create vm
	sudo ./start-vm -ip $IP/16 -gw $GW -tap $TAP -id $VMID -img docker.io/library/nginx:1.17-alpine

	# Wait for vm to boot
	sleep 1

	# Curl nginx
	curl $IP

	# Destroy vm
	sudo ./stop-vm -id $VMID

	# Destroy tap
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/remove_taps.sh $TAP
}

# Build code to manage vms
go build start-vm.go
go build stop-vm.go

test_vm testvm00 testtap00 194.210.228.243 &
#test_vm testvm01 testtap01 194.210.228.244 &
wait
