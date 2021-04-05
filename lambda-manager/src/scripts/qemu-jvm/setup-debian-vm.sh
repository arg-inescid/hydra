#!/bin/bash

# Note: adapted from https://blog.nelhage.com/2013/12/lightweight-linux-kernel-development-with-kvm/

JVM=/home/ubuntu/graalvm-d219e07057-java11-21.1.0-dev

# Build a Wheezy chroot. Install an sshd, since it will be handy
# later.
mkdir stretch
sudo debootstrap --include=openssh-server stretch stretch 

# Perform some manual cleanup on the resulting chroot:

# Make root passwordless for convenience.
sudo sed -i '/^root/ { s/:x:/::/ }' stretch/etc/passwd
# Add a getty on the virtio console
echo 'V0:23:respawn:/sbin/getty 115200 hvc0' | sudo tee -a stretch/etc/inittab
# Set up my ssh pubkey for root in the VM
sudo mkdir stretch/root/.ssh/
cat id_rsa.pub | sudo tee stretch/root/.ssh/authorized_keys

# Build a disk image
dd if=/dev/zero of=stretch.img bs=1M seek=4095 count=1
mkfs.ext4 -F stretch.img
sudo mkdir -p /mnt/stretch
sudo mount -o loop stretch.img /mnt/stretch
sudo cp -a stretch/. /mnt/stretch/.
sudo cp -a $JVM /mnt/stretch/
sudo cp ./startup.sh /mnt/stretch/
sudo cp ./rc.local /mnt/stretch/etc/
sudo umount /mnt/stretch

# At this point, you can delete the "stretch" directory. I usually
# keep it around in case I've messed up the VM image and want to
# recreate it.
