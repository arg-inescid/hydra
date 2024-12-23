#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

LAMBDA_MANAGER_ADDRESS="$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT"

start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
sleep 5

# Upload Java function.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=goshw\&function_language=java\&function_entry_point=null\&function_memory=1024\&function_runtime=graalos\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/../graalos/benchmarks/graalos-client/apps/simple-http/build/native/nativeCompile/simple-http"

APP_POST=/tmp/app-post
echo '{}' > $APP_POST

ab -p $APP_POST -T application/json -c 10 -n 1000 http://$LAMBDA_MANAGER_ADDRESS/user/goshw

echo -e "\n\nFINISHED FIRST ITERATION\n\n"
sleep 10
echo -e "\n\nNEXT ITERATION\n\n"

ab -p $APP_POST -T application/json -c 10 -n 500 http://$LAMBDA_MANAGER_ADDRESS/user/goshw

stop_lambda_manager
rm $APP_POST

wait
