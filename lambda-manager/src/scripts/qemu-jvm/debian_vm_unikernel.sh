#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
FILE_FORMAT=raw
print_and_die() {
    echo -e "$1" >&2
    exit 1
}
USAGE=$(cat << USAGE_END
Usage: --memory mem --ip ip --gateway gateway --mask mask --tap tap_name [--console]
       --memory mem - VM RAM memory to pass to QEMU
       --img image - VM image
       --ip ip - VM ip address
       --gateway gateway - VM gateway
       --mask mask - VM network mask
       --shared directory - Sets the shared directory path
       --tap tap_name - Sets the name of the tap used for networking
       --kernel kernel path - path to kernel image
       --console - Enable console output, disabled by default

USAGE_END
)
VMM_MEM=
VMM_IP=
VMM_CONSOLE=
VMM_GATEWAY=
VMM_MASK=
VMM_TAP_NAME=
VMM_MAC=$(printf 'DE:AD:BE:EF:%02X:%02X\n' $((RANDOM%256)) $((RANDOM%256)))
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
    --img)
        if [ "$2" ]; then
            IMAGE_NAME=$2
            shift
        else
            print_and_die "Flag --img requires an additional argument\n$USAGE"
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
    -k | --kernel)
        if [ "$2" ]; then
            KERNEL_PATH=$2
            shift
        else
            print_and_die "Flag --kernel requires an additional argument\n$USAGE"
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
    --mask)
        if [ "$2" ]; then
            VMM_MASK=$2
            shift
        else
            print_and_die "Flag --mask requires an additional argument\n$USAGE"
        fi
        ;;
    -s | --shared)
        if [ "$2" ]; then
            SHARED_DIR=$2
            shift
        else
            print_and_die "Flag --shared requires an additional argument\n$USAGE"
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
    --console)
        VMM_CONSOLE=true
        ;;
    *)
        break;
        ;;
    esac

    shift
done
if [ -z $VMM_MEM ] || [ -z $VMM_GATEWAY ] || [ -z $VMM_MASK ] || [ -z $VMM_TAP_NAME ] || [ -z $IMAGE_NAME ] || [ -z $SHARED_DIR ] || [ -z $VMM_IP ]; then
    print_and_die "$USAGE"
fi
NODEFAULT_ARGS=
if [ -z $VMM_CONSOLE ]; then
    NODEFAULT_ARGS='-nodefaults -no-user-config -vga none'
fi
KERNEL_CONSOLE_ARGS='console=ttyS0'
if [ -z $VMM_CONSOLE ]; then
    KERNEL_CONSOLE_ARGS=
fi
KERNEL_PCI_SWITCH=
NET_DEV_ARGS=
if qemu-system-x86_64 --machine help | grep microvm &> /dev/null; then
    MACHINE_ARGS='-machine microvm,accel=kvm,kernel_irqchip=on'
    if [ -z $VMM_CONSOLE ]; then
        MACHINE_ARGS='-machine microvm,accel=kvm,kernel_irqchip=on'
    fi
    KERNEL_PCI_SWITCH=pci=off
    NET_DEV_ARGS="-device virtio-net-device,netdev=net1,mac=$VMM_MAC -device virtio-blk-device,drive=drive"
else
    echo "QEMU microVM machine support missing. Running in a pc machine as fallback."
    MACHINE_ARGS='-machine pc,accel=kvm,kernel_irqchip=on,pit=off,smm=off,smbus=off,sata=off,vmport=off'
    NET_DEV_ARGS="-device virtio-net-pci,netdev=net1,mac=$VMM_MAC -device virtio-blk-pci,drive=drive"
fi

# Passing arguments to init through kernel parameters is a bit tricky. The kernel does not understand the concept of quoted strings.
# For example, passing 'this test' to a program running from a shell will result in one argument being passed: "'this test'".
# However, passing the same arguments to init through kernel parameters results in two arguments being passed: "'this" and "test'".
# To avoid this, and to avoid parameters messing in any way with kernel parameters (i.e. a script takes a parameter "quiet"), each argument is prefixed with a '$'.
# In addition, each argument that contains spaces will have those spaces replaced by a '$', while a '$' will be replaced with '$$'.
# In this way, the arguments will be passed correctly. On the other end, init will revert these transformations and pass the original arguments to the program.
FINAL_ARGS=
for arg in "$@"; do
    arg=${arg//\$/\$\$}
    arg=${arg// /\$}
    arg="\$$arg"
    FINAL_ARGS="$FINAL_ARGS $arg"
done

sudo qemu-system-x86_64 \
                $NODEFAULT_ARGS \
                $MACHINE_ARGS \
                -cpu host,-vmx \
                -kernel $KERNEL_PATH -m $VMM_MEM -smp 1 \
                -append "$KERNEL_CONSOLE_ARGS quiet tsc=reliable no_timer_check root=/dev/vda \
                nomodule rw noapic ${KERNEL_PCI_SWITCH} pci=lastbus=0 quiet ip=$VMM_IP::$VMM_GATEWAY:$VMM_MASK::eth0:none:::  noreplace-smp rcupdate.rcu_expedited=1 \
                $FINAL_ARGS" \
                -nographic \
                -netdev type=tap,id=net1,vhost=on,ifname=$VMM_TAP_NAME,script=no \
                ${NET_DEV_ARGS} \
                -no-acpi \
                -no-hpet \
                -fsdev local,id=fs1,path=$SHARED_DIR,security_model=none \
                -device virtio-9p-device,fsdev=fs1,mount_tag=shared \
                -blockdev driver=$FILE_FORMAT,node-name=drive,file.locking=off,file.driver=file,file.filename=$IMAGE_NAME &
echo "$!" > "$DIR"/lambda.pid
echo "$VMM_IP" > "$DIR"/lambda.ip
wait
