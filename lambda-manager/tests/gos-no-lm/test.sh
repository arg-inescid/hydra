#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

if [ -z "$GRAALOS_SDK_DIR" ]
then
    echo "Please set GRAALOS_SDK_DIR first. It should point to directory containing an unzipped version of the GraalOS SDK."
    exit 1
fi

PORT_DISTANCE=10000

function start_graalos_native {
    lambda_port=$1

    graalos_port=$(($lambda_port + $PORT_DISTANCE))

    lambda_home=$(DIR)/"$lambda_port"
    rm -rf "$lambda_home"
    mkdir "$lambda_home" &> /dev/null

    # Launch the GraalOS process.
    "$GRAALOS_SDK_DIR"/benchmarks/graalos-client/run-graalhost-lm.sh $graalos_port &
    echo "$!" > "$lambda_home"/lambda.pid

    # Wait for GraalOS port open.
    while ! nc -z localhost ${graalos_port}; do
        sleep 0.1
    done

    http_code=""
    declare -i attempts=0
    while [[ ! ${http_code} == "201" ]]; do
        res=$(curl --silent --show-error --write-out "%{http_code}" --data-binary '{ "act": "add_ep", "app": "'$GRAALOS_SDK_DIR'/benchmarks/graalos-client/apps/simple-http/build/native/nativeCompile/simple-http", "ep": 2001, "default_socket": { "port": '$lambda_port' }, "listen_socket": { "port": '$lambda_port' }, "fsroot": "/", "fsmappings": [ { "concrete": "/", "virt": "/" } ], "env": { "application_port": "'$lambda_port'" }, "instances": 1 }' http://localhost:$graalos_port/command)
        http_code=$(echo $res | awk '{print $NF}')
        echo "$res"
        attempts=$((attempts+1))

        if [ "$attempts" -gt "1000" ]; then
            break
        fi
        sleep 0.1
    done
}

function stop_graalos_native {
    lambda_port=$1
    graalos_port=$(($lambda_port + $PORT_DISTANCE))
    lambda_home=$(DIR)/"$lambda_port"

    curl -s http://localhost:$graalos_port/exit

    lambda_pid=$(cat "$lambda_home"/lambda.pid)

    declare -i attempts=0
    terminated=""
    while kill -0 $lambda_pid; do
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
        kill $lambda_pid
    fi
}

APPLICATION_PORT=30010

start_graalos_native $APPLICATION_PORT
# curl http://localhost:$APPLICATION_PORT/helloworld
ab -c 1 -n 10 http://localhost:$APPLICATION_PORT/helloworld
stop_graalos_native $APPLICATION_PORT

start_graalos_native $APPLICATION_PORT
# curl http://localhost:$APPLICATION_PORT/helloworld
ab -c 1 -n 10 http://localhost:$APPLICATION_PORT/helloworld
stop_graalos_native $APPLICATION_PORT



wait
