#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

HYDRA_ADDRESS="localhost:8080"
CONTAINER_NAME="lambda_test"

LAMBDA_MEMORY="8589934592"
CGROUPS_CPU_PERIOD="100000"
LAMBDA_CPU_QUOTA="400000"

function start_container {
    docker run --privileged --rm --name="$CONTAINER_NAME" \
        -e app_dir=/codebase/ \
        -p 8080:8080 \
        -v "$ARGO_HOME"/benchmarks/data/apps:/codebase \
        --memory "$LAMBDA_MEMORY" \
        --cpu-period="$CGROUPS_CPU_PERIOD" \
        --cpu-quota="$LAMBDA_CPU_QUOTA" \
        hydra:latest &> $(DIR)/lambda.log &
}

function start_native {
    app_dir=/tmp/apps "$ARGO_HOME"/hydra/build/native-image/polyglot-proxy &> $(DIR)/lambda.log &
    PID="$!"
}

function stop_container {
    docker container stop "$CONTAINER_NAME"
}

function stop_native {
    kill $PID
}

# Start Hydra.
start_container
sleep 5

# Upload JavaScript function.
curl -X POST $HYDRA_ADDRESS/register?name=test\&language=java\&entrypoint=com.uploader.Uploader\&isBinary=true\&svmid=2\&sandbox=snapshot\&url=http://172.18.0.1:8000/apps/gv-js-uploader.so

# Run the experiment.
APP_POST=/tmp/app-post
echo '{"arguments":"{ \"url\":\"http://172.18.0.1:8000/snap.png\" }","name":"test"}' > $APP_POST
ab -p $APP_POST -T application/json -c 8 -n 5000 http://$HYDRA_ADDRESS/warmup?concurrency=1\&requests=1

# Stop Hydra.
stop_container
wait

rm $APP_POST
