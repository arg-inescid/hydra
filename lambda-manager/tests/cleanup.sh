#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

# This script cleans up Firecracker/Docker lambdas in case Lambda Manager terminated abruptly.
# Warning: this script will terminate all Firecracker VMs!

cd $ARGO_HOME/lambda-manager/src/scripts/devmapper

sudo pkill -9 firecracker
sudo bash $ARGO_HOME/lambda-manager/src/scripts/devmapper/remove_remaining_images.sh

cd -

# Ensure no hanging lambdas nor their components.
sudo pgrep firecracker | wc -l
sudo ip a | grep lmt | wc -l
sudo dmsetup ls
ls -alF /dev/mapper/

docker ps --filter name=lambda_* --filter status=running -aq | xargs docker stop
docker ps --filter name=lambda_* -aq | xargs docker rm
