#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

LAMBDA_MANAGER_ADDRESS="$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT"
FUNCTION_MEMORY=1024
FUNCTION_ISOLATION=true
INVOCATION_COLLOCATION=false

RESULTS_DIR="$(DIR)/ab-results"
rm -r $RESULTS_DIR
mkdir -p $RESULTS_DIR

start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
sleep 5

function upload_function {
    username=$1
    function_name=$2
    language=$3
    entrypoint=$4
    function_code=$5
    runtime="openwhisk"

    curl -s -X POST $LAMBDA_MANAGER_ADDRESS/upload_function?username=$username\&function_name=$function_name\&function_language=$language\&function_entry_point=$entrypoint\&function_memory=$FUNCTION_MEMORY\&function_runtime=$runtime\&function_isolation=$FUNCTION_ISOLATION\&invocation_collocation=$INVOCATION_COLLOCATION \
        -H 'Content-Type: text/plain' \
        --data $function_code
}

function benchmark {
    username=$1
    function_name=$2
    payload=$3

    app_post=/tmp/app-post
    echo $payload > $app_post
    results_file=$RESULTS_DIR/"$username-$function_name.log"

    ab -p $app_post -T application/json -c 1 -n 500 http://$LAMBDA_MANAGER_ADDRESS/$username/$function_name &> $results_file

    res=$(cat $results_file | grep "50%")
    echo -e "${GREEN}Median latency for $function_name:\n$res${NC}"

    rm $app_post
}

upload_function user jvhw java       Hello "$ARGO_HOME/benchmarks/src/java/cr-hello-world/init.json"
upload_function user jvfh java       Main  "$ARGO_HOME/benchmarks/src/java/cr-file-hashing/init.json"
upload_function user jvhr java       Main  "$ARGO_HOME/benchmarks/src/java/cr-httprequest/init.json"
upload_function user jshw javascript main  "$ARGO_HOME/benchmarks/src/javascript/cr-hello-world/init.json"
upload_function user jsdh javascript main  "$ARGO_HOME/benchmarks/src/javascript/cr-dynamic-html/init.json"
upload_function user jsup javascript main  "$ARGO_HOME/benchmarks/src/javascript/cr-uploader/init.json"
upload_function user pyhw python     main  "$ARGO_HOME/benchmarks/src/python/cr-hello-world/init.json"
upload_function user pyup python     main  "$ARGO_HOME/benchmarks/src/python/cr-uploader/init.json"
upload_function user pyco python     main  "$ARGO_HOME/benchmarks/src/python/cr-compression/init.json"

benchmark user jvhw '{ }'
benchmark user jvfh '{"url":"http://172.18.0.1:8000/snap.png"}'
benchmark user jvhr '{"url":"http://172.18.0.1:8000/snap.png"}'
benchmark user jshw '{ }'
benchmark user jsdh '{"url":"http://172.18.0.1:8000/template.html","username":"user","nsize":"10"}'
benchmark user jsup '{"url":"http://172.18.0.1:8000/snap.png"}'
benchmark user pyhw '{ }'
benchmark user pyup '{"url":"http://172.18.0.1:8000/snap.png"}'
benchmark user pyco '{"url":"http://172.18.0.1:8000/video.mp4"}'

stop_lambda_manager
