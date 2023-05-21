#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"


LAMBDA_NAME=$1
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

LAMBDA_MODE=$2
if [ -z "$LAMBDA_MODE" ]; then
  echo "Lambda mode is not present."
  exit 1
fi

LAMBDA_IP=$3
if [ -z "$LAMBDA_IP" ]; then
  echo "Lambda ip is not present."
  exit 1
fi

LAMBDA_PORT=$4
if [ -z "$LAMBDA_PORT" ]; then
  echo "Lambda port is not present."
  exit 1
fi

LAMBDA_HOME=$5
if [ -z "$LAMBDA_HOME" ]; then
  echo "Lambda home is not present."
  exit 1
fi

if [ "$LAMBDA_MODE" == "HOTSPOT_W_AGENT" ]; then
  # Collect configuration.
  curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"jni"}' -o "$LAMBDA_HOME"/config/jni-config.json
  curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"predefined-classes"}' -o "$LAMBDA_HOME"/config/predefined-classes-config.json
  curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"proxy"}' -o "$LAMBDA_HOME"/config/proxy-config.json
  curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"reflect"}' -o "$LAMBDA_HOME"/config/reflect-config.json
  curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"resource"}' -o "$LAMBDA_HOME"/config/resource-config.json
  curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"serialization"}' -o "$LAMBDA_HOME"/config/serialization-config.json

  # We need to provide specific SIGTERM signal for HotSpot with Agent in order to allow
  # the agent to write configuration properly. Graalvisor-based lambdas do not terminate
  # when SIGTERM is provided.
  SIGNAL_OPTION="--signal=SIGTERM"
fi

docker container kill $SIGNAL_OPTION $LAMBDA_NAME
