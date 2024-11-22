#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"


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


PORT_DISTANCE=10000
GRAALOS_PORT=$(($LAMBDA_PORT + $PORT_DISTANCE))

curl -s http://localhost:$GRAALOS_PORT/exit

LAMBDA_PID=$(cat "$LAMBDA_HOME"/lambda.pid)

#while kill -0 $LAMBDA_PID; do
#    sleep 0.5
#done

kill $LAMBDA_PID
