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

# Setting kernel options for vm.
kopts="init=$IMAGE_PATH_ON_DISK quiet rw tsc=reliable ipv6.disable=1 ip=$VMM_IP::$VMM_GATEWAY:$VMM_MASK::eth0:none::: nomodule $KERNEL_CONSOLE_ARGS reboot=k panic=1 pci=off $args"

# Remove the old socket file.
socket=/tmp/$VMM_TAP_NAME.socket
rm $socket &> /dev/null

# Start firecracker.
sudo firecracker --api-sock $socket &
sudo ps --ppid "$!" -o pid= > lambda.pid
echo "$VMM_IP" > lambda.ip

# Configures kernel and its arguments.
sudo curl -s --unix-socket $socket -i \
    -X PUT "http://localhost/boot-source" \
    --data "{
        \"kernel_image_path\": \"${VMM_KERNEL}\",
        \"boot_args\": \"${kopts}\"
    }"

# Configures the rootfs.
sudo curl -s --unix-socket $socket -i \
    -X PUT "http://localhost/drives/rootfs" \
    -d "{
        \"drive_id\": \"rootfs\",
        \"path_on_host\": \"${VMM_DISK}\",
        \"is_root_device\": true,
        \"is_read_only\": false
    }"

# Confiures resources.
sudo curl -s --unix-socket $socket -i \
    -X PUT "http://localhost/machine-config" \
    --data "{
        \"vcpu_count\": ${VMM_CPU},
        \"mem_size_mib\": ${VMM_MEM},
        \"track_dirty_pages\": false
    }"

# Confiures network.
sudo curl -s --unix-socket $socket -i \
    -X PUT 'http://localhost/network-interfaces/eth0' \
    -d "{
        \"iface_id\": \"eth0\",
        \"guest_mac\": \"${VMM_MAC}\",
        \"host_dev_name\": \"${VMM_TAP_NAME}\"
    }"

# Launches vm.
sudo curl -s --unix-socket $socket -i \
    -X PUT "http://localhost/actions" \
    -d "{
        \"action_type\": \"InstanceStart\"
    }"

# What for the vm to terminate.
wait
