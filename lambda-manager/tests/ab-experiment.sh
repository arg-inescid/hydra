#!/bin/bash

# Example usage of this script:
# bash ab-experiment.sh gv|gv-si|gv-sf|ow
#
# This script is a simple Apache Benchmark experiment against Lambda Manager.
# It operates with two Java FileHashing functions.
# NOTE: this script requires the "web" container to be started (see benchmarks/data/start-webserver.sh).

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}


function wait_port {
    host=$1
    port=$2
    while ! nc -z $host $port; do echo "Waiting for $host:$port"; sleep 1; done
}


MODE=$1
LAMBDA_MANAGER_CONFIG=$ARGO_HOME/run/configs/manager/default-lambda-manager.json
LAMBDA_MANAGER_HOST=localhost
LAMBDA_MANAGER_PORT=30009
LAMBDA_MANAGER_ADDRESS="$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT"

if [[ "$MODE" = "gv" ]]; then
    FUNCTION_ISOLATION=false
    INVOCATION_COLLOCATION=true
    ENTRY_POINT="com.filehashing.FileHashing"
    FUNCTION_RUNTIME="graalvisor"
    LANGUAGE="java"
    FUNCTION_CODE="$ARGO_HOME/../benchmarks/src/java/gv-file-hashing/build/libfilehashing.so"
elif [[ "$MODE" = "gv-si" ]]; then
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=false
    ENTRY_POINT="com.filehashing.FileHashing"
    FUNCTION_RUNTIME="graalvisor"
    LANGUAGE="java"
    FUNCTION_CODE="$ARGO_HOME/../benchmarks/src/java/gv-file-hashing/build/libfilehashing.so"
elif [[ "$MODE" = "gv-sf" ]]; then
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=true
    ENTRY_POINT="com.filehashing.FileHashing"
    FUNCTION_RUNTIME="graalvisor"
    LANGUAGE="java"
    FUNCTION_CODE="$ARGO_HOME/../benchmarks/src/java/gv-file-hashing/build/libfilehashing.so"
elif [[ "$MODE" = "ow" ]]; then
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=false
    ENTRY_POINT="Main"
    FUNCTION_RUNTIME="openwhisk"
    LANGUAGE="java"
    FUNCTION_CODE="$ARGO_HOME/../benchmarks/src/java/cr-file-hashing/init.json"
else
    echo "Syntax: <mode>"
	exit 1
fi


# Deploy lambda manager and wait for it to launch.
run deploy lm &
wait_port $LAMBDA_MANAGER_HOST $LAMBDA_MANAGER_PORT

sleep 10

# Configure lambda manager.
curl -s -X POST $LAMBDA_MANAGER_ADDRESS/configure_manager -H 'Content-Type: application/json' --data-binary @"$LAMBDA_MANAGER_CONFIG"

# Upload the functions.
curl -s -X POST localhost:30009/upload_function?username=serhii\&function_name=fh1\&function_language="$LANGUAGE"\&function_entry_point="$ENTRY_POINT"\&function_memory=256\&function_runtime="$FUNCTION_RUNTIME"\&function_isolation=$FUNCTION_ISOLATION\&invocation_collocation=$INVOCATION_COLLOCATION -H 'Content-Type: application/octet-stream' --data-binary "$FUNCTION_CODE"
curl -s -X POST localhost:30009/upload_function?username=serhii\&function_name=fh2\&function_language="$LANGUAGE"\&function_entry_point="$ENTRY_POINT"\&function_memory=256\&function_runtime="$FUNCTION_RUNTIME"\&function_isolation=$FUNCTION_ISOLATION\&invocation_collocation=$INVOCATION_COLLOCATION -H 'Content-Type: application/octet-stream' --data-binary "$FUNCTION_CODE"

APP_POST=/tmp/app-post
echo '{"url":"http://172.18.0.1:8000/bacillus_subtilis.fasta","username":"rbruno","nsize":"10"}' > $APP_POST

ab -p $APP_POST -T application/json -c 320 -n 30000 http://$LAMBDA_MANAGER_ADDRESS/serhii/fh1 > /tmp/vm-size-experiment.log
sleep 90
echo -e "\n\nNEXT ITERATION\n\n"
ab -p $APP_POST -T application/json -c 160 -n 15000 http://$LAMBDA_MANAGER_ADDRESS/serhii/fh2 >> /tmp/vm-size-experiment.log

echo "Finished execution, safe to terminate Lambda Manager."
sleep 30
sudo kill $(sudo lsof -i -P -n | grep LISTEN | grep $LAMBDA_MANAGER_PORT | awk '{print $2}')
rm $APP_POST
