#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

LAMBDA_MANAGER_ADDRESS="$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT"

start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
sleep 5

# Upload Java function.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvhw\&function_language=java\&function_entry_point=com.hello_world.HelloWorld\&function_memory=256\&function_runtime=hydra\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=isolate -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-jv-hello-world.so"
# Upload JavaScript function.
# curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jshw\&function_language=java\&function_entry_point=com.helloworld.HelloWorld\&function_memory=256\&function_runtime=hydra\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=snapshot\&svm_id=1 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-js-hello-world.so"
# Upload Python function.
# curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=pyhw\&function_language=java\&function_entry_point=com.helloworld.HelloWorld\&function_memory=256\&function_runtime=hydra\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=snapshot\&svm_id=4 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-py-hello-world.so"

APP_POST=/tmp/app-post
echo '{}' > $APP_POST

ab -p $APP_POST -T application/json -c 100 -n 10000 http://$LAMBDA_MANAGER_ADDRESS/user/jvhw
# ab -p $APP_POST -T application/json -c 100 -n 10000 http://$LAMBDA_MANAGER_ADDRESS/user/jshw
# ab -p $APP_POST -T application/json -c 100 -n 10000 http://$LAMBDA_MANAGER_ADDRESS/user/pyhw

echo -e "\n\nFINISHED FIRST ITERATION\n\n"
sleep 30
echo -e "\n\nNEXT ITERATION\n\n"

ab -p $APP_POST -T application/json -c 50 -n 5000 http://$LAMBDA_MANAGER_ADDRESS/user/jvhw
# ab -p $APP_POST -T application/json -c 50 -n 5000 http://$LAMBDA_MANAGER_ADDRESS/user/jshw
# ab -p $APP_POST -T application/json -c 50 -n 5000 http://$LAMBDA_MANAGER_ADDRESS/user/pyhw

stop_lambda_manager
rm $APP_POST
