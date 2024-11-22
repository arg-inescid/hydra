#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

if [ -z "$GRAALOS_SDK_DIR" ]
then
    echo "Please set GRAALOS_SDK_DIR first. It should point to directory containing an unzipped version of the GraalOS SDK."
    exit 1
fi

LAMBDA_PORT=$1
if [ -z "$LAMBDA_PORT" ]; then
  echo "Lambda port is not present."
  exit 1
fi

LAMBDA_NAME=$2
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

PORT_DISTANCE=10000
GRAALOS_PORT=$(($LAMBDA_PORT + $PORT_DISTANCE))

LAMBDA_HOME="$CODEBASE_HOME"/"$LAMBDA_NAME"
mkdir "$LAMBDA_HOME" &> /dev/null

# Launch the GraalOS process.
"$GRAALOS_SDK_DIR"/benchmarks/graalos-client/run-graalhost-lm.sh $GRAALOS_PORT &
echo "$!" > "$LAMBDA_HOME"/lambda.pid

# Wait for GraalOS port open.
while ! nc -z localhost ${GRAALOS_PORT}; do
    sleep 0.1
done

sleep 0.5

http_code=""
declare -i attempts=0
while [[ ! ${http_code} == "201" ]]; do
    res=$(curl --silent --show-error --write-out "%{http_code}" --data-binary '{ "act": "add_ep", "app": "'$GRAALOS_SDK_DIR'/benchmarks/graalos-client/apps/simple-http/build/native/nativeCompile/simple-http", "ep": 2001, "default_socket": { "port": '$LAMBDA_PORT' }, "listen_socket": { "port": '$LAMBDA_PORT' }, "fsroot": "/", "fsmappings": [ { "concrete": "/", "virt": "/" } ], "env": { "application_port": "'$LAMBDA_PORT'" }, "instances": 1 }' http://localhost:$GRAALOS_PORT/command)
    http_code=$(echo $res | awk '{print $NF}')
    echo "$res"
    attempts=$((attempts+1))

    if [ "$attempts" -gt "1000" ]; then
        break
    fi

    sleep 0.5
done
echo $res

wait
