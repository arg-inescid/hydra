#!/bin/bash

# sudo bash /home/ubuntu/fulltest/lambda-manager/src/scripts/create_taps.sh 10.0.123.180 tap999
./debian-vm_unikernel.sh --memory 512M --gateway 10.0.64.1 --ip 10.0.123.180 --mask 255.255.192.0 --shared /home/ubuntu/virt-bench/debian-vm/shared --tap tap999 --console
# destroy tap
# Run some command on the vm
#ssh -oStrictHostKeyChecking=no -i id_rsa root@10.0.123.180
