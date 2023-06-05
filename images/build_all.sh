#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

function build_image {
    IMAGE=$1
    cd "$DIR"/"$IMAGE"
    bash build.sh "$IMAGE".img
    rm "$DIR"/"$IMAGE"/base.ext4 "$DIR"/"$IMAGE"/init "$DIR"/"$IMAGE"/init.o
    cd "$DIR"
}

build_image java_openwhisk
build_image hotspot
build_image hotspot-agent
