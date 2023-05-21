#!/bin/bash

# This script creates a Docker image that can be used to:
# 1. Build Native Image binaries;
# 2. Run HotSpot-based lambdas.

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

# Remove previous build of the image.
docker image rm argo-builder:latest

DISK=$DIR/disk
mkdir -p $DISK

GRAALVISOR_PROXY_JAR=$DIR/../graalvisor/build/libs/graalvisor-1.0-all.jar

# Copy Graalvisor proxy JAR.
cp $GRAALVISOR_PROXY_JAR $DISK
# Copy GraalVM.
cp -r $JAVA_HOME $DISK/jvm
# Copy caller filter configuration for agent.
cp $DIR/../lambda-manager/src/main/resources/caller-filter-config.json $DISK
# Prepare initial empty config for agent.
# TODO: initial config (empty or from previous agent) should come from start_hotspot script.
mkdir $DISK/config
printf "[\n]\n" > $DISK/config/jni-config.json
printf "[\n]\n" > $DISK/config/predefined-classes-config.json
printf "[\n]\n" > $DISK/config/proxy-config.json
printf "[\n]\n" > $DISK/config/reflect-config.json
printf "{\n}\n" > $DISK/config/resource-config.json
printf "[\n]\n" > $DISK/config/serialization-config.json

docker build --tag=argo-builder $DIR

rm -rf $DISK
