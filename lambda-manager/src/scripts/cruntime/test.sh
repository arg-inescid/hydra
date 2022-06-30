#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

ARGO_HOME=$(DIR)/../../../../
TAP=tap001
IP=10.0.0.200
GW=10.0.0.201
SMASK=16

# Build code to manage vms
go build start-vm.go
go build stop-vm.go

# Create tap
sudo bash $ARGO_HOME/lambda-manager/src/scripts/create_taps.sh $TAP $IP

# Create vm
sudo ./start-vm -ip $IP/16 -gw $GW -tap $TAP -id fc-example2 -img docker.io/library/nginx:1.17-alpine

# Wait for vm to boot
sleep 1

# Curl nginx
curl $IP

# Destroy vm
sudo ./stop-vm -id fc-example2

# Destroy tap
sudo bash $ARGO_HOME/lambda-manager/src/scripts/remove_taps.sh $TAP
