#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# sudo bash /home/ubuntu/fulltest/lambda-manager/src/scripts/create_taps.sh tap999 10.0.123.180
./debian-vm_unikernel.sh --memory 512M --gateway 192.168.1.81 --ip 192.168.1.82 --mask 255.255.255.0 --img $DIR/stretch.img --shared $DIR/shared --tap tap999 --console
# destroy tap
# ssh -oStrictHostKeyChecking=no -i id_rsa root@10.0.123.180
