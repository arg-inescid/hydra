#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

function delete_base {
  BASENAME=$1
  
  sudo losetup --detach $(cat $DIR/$BASENAME.loop)
  sudo dmsetup remove $BASENAME
  rm $DIR/$BASENAME.loop
}

declare -a image_names=("graalvisor" "hotspot" "hotspot-agent" "java-openwhisk" "javascript-openwhisk" "python-openwhisk")

for image in "${image_names[@]}"
do
  delete_base "$image"
done
