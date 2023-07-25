#!/bin/bash

print_and_die() {
    echo -e "$1" >&2
    exit 1
}

USAGE=$(cat << USAGE_END
Usage: --disk disk --kernel kernel --memory mem --ip ip --gateway gateway --mask mask --tap tapname [--console]
       --disk disk - path to the VM disk image
       --kernel kernel - path to the VM kernel image
       --memory mem - VM RAM memory
       --cpu cpu - Number of cpus
       --ip ip - VM ip address
       --gateway gateway - VM gateway
       --mask mask - VM networm mask
       --tap tapname - Sets the name of the tap used for networking
       --console - Enable console output, disabled by default

USAGE_END
)

IMAGE_PATH_ON_DISK="/init"
VMM_KERNEL=
VMM_DISK=
VMM_MEM=
VMM_CPU=1
VMM_IP=
VMM_CONSOLE=
VMM_GATEWAY=
VMM_MASK=
VMM_TAP_NAME=
VMM_MAC=`printf 'DE:AD:BE:EF:%02X:%02X\n' $((RANDOM%256)) $((RANDOM%256))`

while :; do
    case $1 in
    -h | --help)
        print_and_die "$USAGE"
        ;;
    -m | --memory)
        if [ "$2" ]; then
            VMM_MEM=$2
            shift
        else
            print_and_die "Flag --memory requires an additional argument\n$USAGE"
        fi
        ;;
    -c | --cpu)
        if [ "$2" ]; then
            VMM_CPU=$2
            shift
        else
            print_and_die "Flag --cpu requires an additional argument\n$USAGE"
        fi
        ;;
    -i | --ip)
        if [ "$2" ]; then
            VMM_IP=$2
            shift
        else
            print_and_die "Flag --ip requires an additional argument\n$USAGE"
        fi
        ;;
    -g | --gateway)
        if [ "$2" ]; then
            VMM_GATEWAY=$2
            shift
        else
            print_and_die "Flag --gateway requires an additional argument\n$USAGE"
        fi
        ;;
    -k | --mask)
        if [ "$2" ]; then
            VMM_MASK=$2
            shift
        else
            print_and_die "Flag --mask requires an additional argument\n$USAGE"
        fi
        ;;
    -t | --tap)
        if [ "$2" ]; then
            VMM_TAP_NAME=$2
            shift
        else
            print_and_die "Flag --tap requires an additional argument\n$USAGE"
        fi
        ;;
    -d | --disk)
        if [ "$2" ]; then
            VMM_DISK=$2
            shift
        else
            print_and_die "Flag --disk requires an additional argument\n$USAGE"
        fi
        ;;
    -l | --kernel)
        if [ "$2" ]; then
            VMM_KERNEL=$2
            shift
        else
            print_and_die "Flag --kernel requires an additional argument\n$USAGE"
        fi
        ;;
    --console)
        VMM_CONSOLE=true
        ;;
    *)
        break;
        ;;
    esac

    shift
done

# Check if all mandatory arguments are present.
if [ -z $VMM_MEM ] || [ -z $VMM_GATEWAY ] || [ -z $VMM_MASK ] || [ -z $VMM_TAP_NAME ] || [ -z $VMM_IP ] || [ -z $VMM_DISK ] || [ -z $VMM_KERNEL ]; then
    print_and_die "$USAGE"
fi

# Switch for using a console.
KERNEL_CONSOLE_ARGS='console=ttyS0'
if [ -z $VMM_CONSOLE ]; then
    KERNEL_CONSOLE_ARGS=
fi

# Setting args for vm.
args=$@

# Remove the old socket file.
rm /tmp/$VMM_TAP_NAME.socket &> /dev/null

# Kernel opts example: https://github.com/firecracker-microvm/firecracker-demo/blob/main/start-firecracker.sh
firectl \
        --kernel=$VMM_KERNEL \
        --kernel-opts="init=$IMAGE_PATH_ON_DISK quiet rw tsc=reliable ipv6.disable=1 ip=$VMM_IP::$VMM_GATEWAY:$VMM_MASK::eth0:none::: nomodule $KERNEL_CONSOLE_ARGS reboot=k panic=1 pci=off $args" \
        --root-drive=$VMM_DISK \
        --memory=$VMM_MEM \
        --ncpus=$VMM_CPU \
        --tap-device=$VMM_TAP_NAME/$VMM_MAC \
        --socket-path=/tmp/$VMM_TAP_NAME.socket &
echo "$!" > lambda.pid
echo "$VMM_IP" > lambda.ip
wait
