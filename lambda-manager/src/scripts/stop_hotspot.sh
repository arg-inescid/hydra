#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/prepare_lambda_directories.sh

LAMBDA_HOME=$1
if [ -z "$LAMBDA_HOME" ]; then
  echo "Lambda home is not present."
  exit 1
fi

LAMBDA_PORT=$2
if [ -z "$LAMBDA_PORT" ]; then
  echo "Lambda port is not present."
  exit 1
fi

LAMBDA_MODE=$3
if [ -z "$LAMBDA_MODE" ]; then
  echo "Lambda mode is not present."
  exit 1
fi

if [ "$LAMBDA_MODE" == "HOTSPOT_W_AGENT" ]; then
  # Collect configuration.
  curl -s -X POST $(cat "$LAMBDA_HOME"/lambda.ip):"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"jni"}' -o "$LAMBDA_HOME"/config/jni-config.json
  curl -s -X POST $(cat "$LAMBDA_HOME"/lambda.ip):"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"predefined-classes"}' -o "$LAMBDA_HOME"/config/predefined-classes-config.json
  curl -s -X POST $(cat "$LAMBDA_HOME"/lambda.ip):"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"proxy"}' -o "$LAMBDA_HOME"/config/proxy-config.json
  curl -s -X POST $(cat "$LAMBDA_HOME"/lambda.ip):"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"reflect"}' -o "$LAMBDA_HOME"/config/reflect-config.json
  curl -s -X POST $(cat "$LAMBDA_HOME"/lambda.ip):"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"resource"}' -o "$LAMBDA_HOME"/config/resource-config.json
  curl -s -X POST $(cat "$LAMBDA_HOME"/lambda.ip):"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data '{"configName":"serialization"}' -o "$LAMBDA_HOME"/config/serialization-config.json
fi

sudo ssh -o StrictHostKeyChecking=no -i "$LAMBDA_HOME"/id_rsa root@"$(cat "$LAMBDA_HOME"/lambda.ip)" pkill java
sleep 1 # Give some time for Java Agent to write configuration.

sudo pkill -TERM -P "$(cat "$LAMBDA_HOME"/lambda.pid)"
sudo kill -TERM "$(cat "$LAMBDA_HOME"/lambda.pid)"
