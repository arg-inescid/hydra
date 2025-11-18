#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

LAMBDA_MANAGER_ADDRESS="$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT"

start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
sleep 5

# Upload JavaScript function.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jsup\&function_language=java\&function_entry_point=com.uploader.Uploader\&function_memory=1024\&function_runtime=hydra\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=snapshot\&svm_id=2 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-js-uploader.so"

APP_POST=/tmp/app-post
echo '{"url":"http://172.18.0.1:8000/snap.png"}' > $APP_POST

ab -p $APP_POST -T application/json -c 8 -n 5000 http://$LAMBDA_MANAGER_ADDRESS/user/jsup

stop_lambda_manager
rm $APP_POST
