#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

function prepare_base {
  BASENAME=$1
  BASEIMAGE=$2
  SZ=`sudo blockdev --getsz $BASEIMAGE`
  echo $SZ
  # Step 1: Create a loop device for the BASEIMAGE file (like /dev/loop16)
  LOOP=$(sudo losetup --find --show --read-only $BASEIMAGE)
  echo "$LOOP" > $DIR/$BASENAME.loop
  # Step 2: Create device mapper for the base image.
  printf "0 $SZ linear $LOOP 0\n$SZ $SZ zero" | sudo dmsetup create $BASENAME
}

MODE=$1

declare -a image_names=("hydra" "hotspot" "hotspot-agent" "java-openwhisk" "javascript-openwhisk" "python-openwhisk")

if [ "$MODE" == "snapshot" ]; then
  # Prepare devmapper for snapshotted images.
  BASE_SNAPSHOT_DIR=$DIR/../../../../images/snapshots
  for image in "${image_names[@]}"
  do
    IMAGE_PATH="$BASE_SNAPSHOT_DIR"/"$image"/172.18.0.3/root/"$image".img
    prepare_base "$image" "$IMAGE_PATH"
  done
else
  # Prepare devmapper for normal images.
  BASE_IMAGE_DIR=$DIR/../../../../images
  for image in "${image_names[@]}"
  do
    IMAGE_PATH="$BASE_IMAGE_DIR"/"$image"/"$image".img
    prepare_base "$image" "$IMAGE_PATH"
  done
fi

