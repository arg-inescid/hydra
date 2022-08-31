#!/bin/bash

print_and_die() {
    echo -e "$1" >&2
    exit 1
}

USAGE=$(cat << USAGE_END
Usage: --vmm vmm --disk disk --kernel kernel --memory mem --ip ip --gateway gateway --mask mask --tap tapname [--console]
       --vmm vmm - firecracker or qemu
       --disk disk - path to the VM disk image
       --kernel kernel - path to the VM kernel image
       --memory mem - VM RAM memory
       --ip ip - VM ip address
       --gateway gateway - VM gateway
       --mask mask - VM networm mask
       --tap tapname - Sets the name of the tap used for networking
       --console - Enable console output, disabled by default

USAGE_END
)

IMAGE_PATH_ON_DISK="/init"
FILE_FORMAT="raw"
VMM=
VMM_KERNEL=
VMM_DISK=
VMM_MEM=
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
    -v | --vmm)
        if [ "$2" ]; then
            VMM=$2
            shift
        else
            print_and_die "Flag --vmm requires an additional argument\n$USAGE"
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
if [ -z $VMM ] || [ -z $VMM_MEM ] || [ -z $VMM_GATEWAY ] || [ -z $VMM_MASK ] || [ -z $VMM_TAP_NAME ] || [ -z $VMM_IP ] || [ -z $VMM_DISK ] || [ -z $VMM_KERNEL ]; then
    print_and_die "$USAGE"
fi

# Switch for using a console.
KERNEL_CONSOLE_ARGS='console=ttyS0'
if [ -z $VMM_CONSOLE ]; then
    KERNEL_CONSOLE_ARGS=
fi

# Setting args for vm.
args=$@

function run_firecracker {
    rm $VMM_DISK.sock &> /dev/null


    # Kernel opts example: https://github.com/firecracker-microvm/firecracker-demo/blob/main/start-firecracker.sh
    firectl \
            --kernel=$VMM_KERNEL \
            --kernel-opts="init=$IMAGE_PATH_ON_DISK quiet rw tsc=reliable ipv6.disable=1 ip=$VMM_IP::$VMM_GATEWAY:$VMM_MASK::eth0:none::: nomodule $KERNEL_CONSOLE_ARGS reboot=k panic=1 pci=off $args" \
            --root-drive=$VMM_DISK \
            --memory=$VMM_MEM \
            --tap-device=$VMM_TAP_NAME/$VMM_MAC \
            --socket-path=$VMM_DISK.socket &
    echo "$!" > lambda.pid
    echo "$VMM_IP" > lambda.ip
    wait
}

function run_qemu {
    NODEFAULT_ARGS=
    if [ -z $VMM_CONSOLE ]; then
        NODEFAULT_ARGS='-nodefaults -no-user-config -vga none'
    fi
    MACHINE_ARGS='-machine microvm,accel=kvm,kernel_irqchip=off'
    KERNEL_PCI_SWITCH=pci=off
    NET_DEV_ARGS="-device virtio-net-device,netdev=net1,mac=$VMM_MAC -device virtio-blk-device,drive=drive"

    sudo qemu-system-x86_64 \
        $NODEFAULT_ARGS \
        $MACHINE_ARGS \
        -cpu host,-vmx \
        -kernel $VMM_KERNEL -m $VMM_MEM -smp 1 \
        -append "$KERNEL_CONSOLE_ARGS quiet tsc=reliable init=${IMAGE_PATH_ON_DISK} no_timer_check root=/dev/vda nomodule rw noapic ${KERNEL_PCI_SWITCH} pci=lastbus=0 quiet ip=$VMM_IP::$VMM_GATEWAY:$VMM_MASK::eth0:none:::  noreplace-smp rcupdate.rcu_expedited=1 \
        $args" \
        -nographic \
        -netdev type=tap,id=net1,vhost=on,ifname=$VMM_TAP_NAME,script=no \
        ${NET_DEV_ARGS} \
        -no-acpi \
        -no-hpet \
        -blockdev driver=$FILE_FORMAT,node-name=drive,file.locking=off,file.driver=file,file.filename=$VMM_DISK &
        echo "$!" > lambda.pid
        echo "$VMM_IP" > lambda.ip
        wait
}

if [ "$VMM" == "firecracker" ]; then
    run_firecracker
elif [ "$VMM" == "qemu" ]; then
    run_qemu
else
    print_and_die "Unknown VMM: $VMM."
fi
