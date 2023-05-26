#!/bin/bash

# This script creates Docker images that can be used to run HotSpot lambdas.

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

DISK=$DIR/disk

JAVA_HOME=$1
GRAALVISOR_JAR=$2

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters."
    echo "Syntax: build_hotspot_container_image.sh <graalvm home> <input graalvisor jar path>"
    exit 1
fi

docker image rm argo-hotspot:latest
docker image rm argo-hotspot-agent:latest

rm -rf $DISK &> /dev/null
mkdir -p $DISK

# Copy Graalvisor proxy JAR.
cp $GRAALVISOR_JAR $DISK
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

GRAALVISOR_JAR_FILENAME="$(basename -- $GRAALVISOR_JAR)"
GRAALVISOR_ENTRYPOINT="org.graalvm.argo.graalvisor.Main"

ENTRYPOINT_COMMAND="/jvm/bin/java -cp $GRAALVISOR_JAR_FILENAME $GRAALVISOR_ENTRYPOINT"
echo "$ENTRYPOINT_COMMAND"
docker build -f $DIR/HotSpot.Dockerfile \
    --target argo-hotspot \
    --build-arg ENTRYPOINT_COMMAND="$ENTRYPOINT_COMMAND" \
    --tag=argo-hotspot $DIR

AGENT_OPTIONS="-agentlib:native-image-agent=config-merge-dir=config,caller-filter-file=caller-filter-config.json,config-write-initial-delay-secs=20,config-write-period-secs=300"
ENTRYPOINT_COMMAND="/jvm/bin/java -Djava.library.path=/jvm/lib $AGENT_OPTIONS -cp $GRAALVISOR_JAR_FILENAME $GRAALVISOR_ENTRYPOINT"
echo "$ENTRYPOINT_COMMAND"
docker build -f $DIR/HotSpot.Dockerfile \
    --target argo-hotspot-agent \
    --build-arg ENTRYPOINT_COMMAND="$ENTRYPOINT_COMMAND" \
    --tag=argo-hotspot-agent $DIR

rm -rf $DISK
