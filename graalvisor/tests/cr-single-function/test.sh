#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function start_native {
    export app_dir=$(DIR)
    bash $(DIR)/../../graalvisor $(DIR)/graalvisor.pid &>> $(DIR)/graalvisor.log &
}

function stop_native {
    kill $(cat $(DIR)/graalvisor.pid)
}

# TODO - parametrize function to use.

# Ensure that we have a clean environment to start.
bash $(DIR)/cleanup.sh

# Run and checkpoint.
start_native
sleep 1 # TODO - proper wait for port
curl -X POST localhost:8080/register?name=test\&language=java\&entrypoint=com.uploader.Uploader\&isBinary=true\&svmid=2\&sandbox=snapshot\&url=http://localhost:8000/apps/gv-js-uploader.so
ab -p $(DIR)/app-post -T application/json -c 1 -n 1000 http://localhost:8080/warmup?concurrency=1\&requests=1
stop_native
wait

# Restore and run.
start_native
sleep 1 # TODO - proper wait for port
curl -X POST localhost:8080/register?name=test\&language=java\&entrypoint=com.uploader.Uploader\&isBinary=true\&svmid=2\&sandbox=snapshot\&url=http://localhost:8000/apps/gv-js-uploader.so
ab -p $(DIR)/app-post -T application/json -c 1 -n 1000 http://localhost:8080/warmup?concurrency=1\&requests=1
stop_native
wait
