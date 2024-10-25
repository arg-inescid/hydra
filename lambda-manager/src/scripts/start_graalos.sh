#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

if [ -z "$GRAALOS_SDK_DIR" ]
then
    echo "Please set GRAALOS_SDK_DIR first. It should point to directory conainting an unziped version of the Graal OS SDK."
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

CONTAINER_IMAGE="graalos-image"
PORT_DISTANCE=10000
GRAALOS_PORT=$(($LAMBDA_PORT + $PORT_DISTANCE))

LAMBDA_HOME="$CODEBASE_HOME"/"$LAMBDA_NAME"
mkdir "$LAMBDA_HOME" &> /dev/null

cd "$LAMBDA_HOME"

docker run --rm --name $LAMBDA_NAME \
    -p $LAMBDA_PORT:9001 \
    -p $GRAALOS_PORT:$GRAALOS_PORT \
    -v "${GRAALOS_SDK_DIR}:/graalos" \
    -v "/home/sergiyivan/work/graalos/graalos-benchmarks:/graalos-benchmarks" \
    -e GRAALOS_SDK_DIR="/graalos" \
    -w "/graalos-benchmarks/graalos-client/" \
    --user 1274:1145 \
    $CONTAINER_IMAGE \
    ./run-graalhost-lm.sh $GRAALOS_PORT &

# Wait for GraalOS port open.
while ! nc -z localhost ${GRAALOS_PORT}; do
    sleep 0.1
done

sleep 0.5

res=""
while [[ ! ${res} == *"AddEndpoint"* ]]; do
    res=$(docker exec $LAMBDA_NAME curl --silent --show-error --write-out "%{http_code}" --data-binary '{ "act": "add_ep", "app": "/graalos-benchmarks/graalos-client/apps/simple-http/build/native/nativeCompile/simple-http", "ep": 2001, "default_socket": { "port": 9001 }, "listen_socket": { "port": 9001 }, "fsroot": "/", "fsmappings": [ { "concrete": "/", "virt": "/" } ], "env": { "myvar": "initial_value" }, "instances": 1 }' http://localhost:$GRAALOS_PORT/command)
done
echo $res

wait
