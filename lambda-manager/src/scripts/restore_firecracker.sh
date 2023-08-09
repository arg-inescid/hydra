#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

LAMBDA_ID="$1"
if [ -z "$LAMBDA_ID" ]; then
  echo "Lambda id is not present."
  exit 1
fi

LAMBDA_IP="$2"
if [ -z "$LAMBDA_IP" ]; then
  echo "Lambda ip is not present."
  exit 1
fi

LAMBDA_NAME="$3"
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

VM_IMAGE="$4"
if [ -z "$VM_IMAGE" ]; then
  echo "VM image is not present."
  exit 1
fi

LAMBDA_HOME="$CODEBASE_HOME"/"$LAMBDA_NAME"
mkdir "$LAMBDA_HOME" &> /dev/null

FIRECRACKER_ID=lambda"$LAMBDA_ID"id
CHROOT_DIR=$LAMBDA_HOME/chroot

function start_jailer {
    mkdir -p $CHROOT_DIR/firecracker/$FIRECRACKER_ID/root/
    touch    $CHROOT_DIR/firecracker/$FIRECRACKER_ID/root/firecracker.log

    # create namespace
    sudo ip netns add ns$LAMBDA_ID

    sudo jailer \
        --id $FIRECRACKER_ID \
        --exec-file $(which firecracker) \
        --uid 0 \
        --gid 0 \
        --netns /var/run/netns/ns$LAMBDA_ID \
        --chroot-base-dir $CHROOT_DIR \
        -- \
        --api-sock firecracker.socket \
        --log-path firecracker.log \
        --level Debug \
        --show-level \
        --show-log-origin &
}

function gen_hostns_veth_ip {
    # 10.<idx / 30>.<(idx % 30) * 8>.1/24
    byte1=10
    byte2=$(($LAMBDA_ID / 30))
    byte3=$(($LAMBDA_ID % 30))
    byte3=$(($byte3 * 8))
    byte4=1
    echo "$byte1.$byte2.$byte3.$byte4"
}

function gen_vmns_veth_ip {
    # 10.<idx / 30>.<(idx % 30) * 8>.2/24
    byte1=10
    byte2=$(($LAMBDA_ID / 30))
    byte3=$(($LAMBDA_ID % 30))
    byte3=$(($byte3 * 8))
    byte4=2
    echo "$byte1.$byte2.$byte3.$byte4"
}

function bind_mount {
  SRC=$1
  DST=$2
  MOUNT_MODE=$3
  sudo touch "$DST"
  sudo mount --bind "$SRC" "$DST"
  if [ "$MOUNT_MODE" == "read-only" ]; then
    sudo mount -o bind,remount,ro "$DST"
  fi
}

function load_snapshot {
    # Snapshot file paths (files inside the chroot).
    VM_SNAP_FILE=snapshot_file
    VM_SNAP_MEM=mem_file

    SNAPSHOT_DIR=$DIR/../../../images/snapshots/$VM_IMAGE/172.18.0.3/root

    # Instead of just copying an image, we create an overlay for it to use devmapper.
    bash "$DIR"/devmapper/prepare_overlay_image.sh \
        "$VM_IMAGE" \
        "$SNAPSHOT_DIR"/"$VM_IMAGE".img \
        "$LAMBDA_NAME" \
        "$VM_DIR"/"$VM_IMAGE".img.overlay

    # Creating a bind mount for disk image to work with devmapper.
    bind_mount /dev/mapper/"$LAMBDA_NAME" "$VM_DIR"/"$VM_IMAGE".img

    # Bind mounting the kernel file to the VM directory.
    bind_mount "$SNAPSHOT_DIR"/hello-vmlinux.bin "$VM_DIR"/hello-vmlinux.bin read-only
    # Bind mounting the snapshot files to the VM directory.
    bind_mount "$SNAPSHOT_DIR"/"$VM_SNAP_FILE" "$VM_DIR"/"$VM_SNAP_FILE" read-only
    bind_mount "$SNAPSHOT_DIR"/"$VM_SNAP_MEM" "$VM_DIR"/"$VM_SNAP_MEM" read-only

    sudo curl --unix-socket $VM_SOCKET -i \
        -X PUT "http://localhost/snapshot/load" \
        -d "{
        \"snapshot_path\": \"$VM_SNAP_FILE\",
        \"mem_file_path\": \"$VM_SNAP_MEM\",
        \"enable_diff_snapshots\": false,
        \"resume_vm\": true
        }"
}

start_jailer

# Internal (vm tap ip) and external (host tap ip) ips and masks (same for all clones).
HOST_TAP_IP=192.168.241.1
VM_TAP_IP=192.168.241.2
TAP=vmtap
TAP_MASK_SHORT=29
TAP_MASK_LONG=255.255.255.248

# Veth ips, mask, and names both in the host and in the vm namespaces.
HOST_NS_VETH_IP=$(gen_hostns_veth_ip)
HOST_NS_VETH=veth$HOST_NS_VETH_IP
VM_NS_VETH_IP=$(gen_vmns_veth_ip)
VM_NS_VETH=veth$VM_NS_VETH_IP
VETH_MASK_SHORT=24

# Default network device and mac used in the vm.
VM_DEV=eth0
VM_MAC=$(printf 'DE:AD:BE:EF:%02X:%02X\n' $((RANDOM%256)) $((RANDOM%256)))

# Create vm tap in vm namespace.
sudo ip netns exec ns$LAMBDA_ID ip tuntap add dev $TAP mode tap
sudo ip netns exec ns$LAMBDA_ID ip addr add $HOST_TAP_IP/$TAP_MASK_SHORT dev $TAP
sudo ip netns exec ns$LAMBDA_ID ip link set dev $TAP up

# Create vm veth pair.
sudo ip netns exec ns$LAMBDA_ID ip link add $HOST_NS_VETH type veth peer name $VM_NS_VETH
sudo ip netns exec ns$LAMBDA_ID ip addr add $VM_NS_VETH_IP/$VETH_MASK_SHORT dev $VM_NS_VETH
sudo ip netns exec ns$LAMBDA_ID ip link set dev $VM_NS_VETH up

# Move one end to the host namespace.
sudo ip netns exec ns$LAMBDA_ID ip link set $HOST_NS_VETH netns 1
sudo ip addr add $HOST_NS_VETH_IP/$VETH_MASK_SHORT dev $HOST_NS_VETH
sudo ip link set dev $HOST_NS_VETH up

# Designate the outer end as default gateway for packets leaving the namespace.
sudo ip netns exec ns$LAMBDA_ID ip route add default via $HOST_NS_VETH_IP dev $VM_NS_VETH

# For packets that leave the namespace and have the source ip address of the
# original guest, rewrite the source address to public clone address.
sudo ip netns exec ns$LAMBDA_ID iptables -t nat -A POSTROUTING -o $VM_NS_VETH -s $VM_TAP_IP -j SNAT --to $LAMBDA_IP

# do the reverse operation; rewrites the destination address of packets
# heading towards the clone address to vm tap ip.
sudo ip netns exec ns$LAMBDA_ID iptables -t nat -A PREROUTING -i $VM_NS_VETH -d $LAMBDA_IP -j DNAT --to $VM_TAP_IP

# Adds a route on the host for the clone address.
sudo ip route add $LAMBDA_IP via $VM_NS_VETH_IP

# Directory where the socket and logs of the vm will be placed (the link is created to avoid long paths).
VM_DIR=$CHROOT_DIR/root
rm $VM_DIR &> /dev/null
sudo ln -s $CHROOT_DIR/firecracker/$FIRECRACKER_ID/root $VM_DIR

# TODO: this is a dirty hack to avoid having socket path too long.
if [ ! -L /tmp/codebase ]; then
    ln -s $CODEBASE_HOME /tmp/codebase
fi

# Socket that will be used to control the vm.
VM_SOCKET=/tmp/codebase/$LAMBDA_NAME/chroot/root/firecracker.socket

load_snapshot

wait
