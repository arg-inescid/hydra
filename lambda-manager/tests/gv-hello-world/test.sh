#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
sleep 5

# Upload Java function.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvhw\&function_language=java\&function_entry_point=com.hello_world.HelloWorld\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=isolate -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-jv-hello-world.so"

# Make a request to the Java function.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jvhw -H 'Content-Type: application/json' --data '{}'

stop_lambda_manager
