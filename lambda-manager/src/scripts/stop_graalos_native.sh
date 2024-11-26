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

declare -i attempts=0
terminated=""
while kill -0 $LAMBDA_PID; do
    attempts=$((attempts+1))
    echo "Attempt #$attempts."
    if [ "$attempts" -gt "100" ]; then
        # Giving up on waiting for a graceful shutdown.
        terminated="false"
        break
    fi

    sleep 0.5
done

# Killing if couldn't shut down gracefully.
if [[ "$terminated" == "false" ]]; then
    echo "Killing the lambda manually."
    kill $LAMBDA_PID
fi
