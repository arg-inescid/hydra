#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
source $DIR/../env.sh

# sudo ../create_taps.sh tap999 192.168.1.250
./debian-vm_unikernel.sh --memory 512M --gateway 192.168.1.1 --ip 192.168.1.250 --mask 255.255.255.0 --kernel $KERNEL_PATH --img stretch.img --shared shared --tap tap999 --console
# sudo ../remove_taps tap999
# ssh -oStrictHostKeyChecking=no -i id_rsa root@10.0.123.180
