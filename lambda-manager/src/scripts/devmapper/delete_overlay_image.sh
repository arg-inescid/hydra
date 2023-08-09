#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

OVERLAYNAME=$1
OVERLAY=$2

sudo losetup --detach $(cat $DIR/$OVERLAYNAME.loop)
sudo dmsetup remove $OVERLAYNAME
rm $DIR/$OVERLAYNAME.loop
rm $OVERLAY
