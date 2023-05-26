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
  mkdir "$LAMBDA_HOME"/config
  for config_name in jni predefined-classes proxy reflect resource serialization; do
    curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data "{\"configName\":\"$config_name\"}" -o "$LAMBDA_HOME"/config/"$config_name"-config.json
  done
fi

docker container kill $SIGNAL_OPTION $LAMBDA_NAME
