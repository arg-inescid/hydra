#!/bin/bash

# This script creates Docker images that can be used to run HotSpot lambdas.
# Important note: make sure you have the "argo-hotspot" image built locally before running this script.

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

HYDRA_JAR=$DIR/../../hydra/build/libs/hydra-1.0-all.jar

docker image rm argo-hotspot-agent:latest

HYDRA_JAR_FILENAME="$(basename -- $HYDRA_JAR)"
HYDRA_ENTRYPOINT="org.graalvm.argo.hydra.Main"
AGENT_OPTIONS="-agentlib:native-image-agent=config-merge-dir=config,caller-filter-file=caller-filter-config.json,config-write-initial-delay-secs=90,config-write-period-secs=60"
ENTRYPOINT_COMMAND="/jvm/bin/java -Djava.library.path=/jvm/lib $AGENT_OPTIONS -cp $HYDRA_JAR_FILENAME $HYDRA_ENTRYPOINT"

docker build \
    --build-arg ENTRYPOINT_COMMAND_AGENT="$ENTRYPOINT_COMMAND" \
    --tag=argo-hotspot-agent $DIR
