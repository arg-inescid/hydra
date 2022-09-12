#!/bin/bash

# Example usage of this script:
# bash benchmark-lm-load.sh gv|cr /path/to/dataset/file
# The structure of the .csv file should be as follows:
# HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}


function process_dataset {
    csv_file=$1
    function_code=$2
    function_name=$3
    function_entry_point=$4
    function_runtime=$5

    # This will be used as a set to know which functions have already been uploaded
    declare -A setUploadedOwners

    current_timestamp=0
    tail -n +2 $csv_file |
    while IFS=, read -r HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp
    do
        if [ -z "${setUploadedOwners[$HashOwner]}" ]
        then
            # Upload function for current owner and set as uploaded to prevent uploading it more than once
            curl -s -X POST $LAMBDA_MANAGER_ADDRESS/upload_function?username=$HashOwner\&function_name=$function_name\&function_language=java\&function_entry_point=$function_entry_point\&function_memory=64\&function_runtime=$function_runtime \
                -H 'Content-Type: application/octet-stream' --data-binary @"$function_code"
            setUploadedOwners["$HashOwner"]=1
        fi

        # This is just to adjust the start of the requests with the beginning of the hour
        time_to_sleep=$(python3 -c "print((($Timestamp - $current_timestamp) % 3600000) / 1000)")
        current_timestamp=$Timestamp
        sleep $time_to_sleep
        curl -s -X POST $LAMBDA_MANAGER_ADDRESS/$HashOwner/$function_name -H 'Content-Type: application/json' --data '{"memory":"32000000","sleep":"'$AverageDuration'"}' &
    done
    wait

    sleep 10
    echo "Finished benchmark execution. Stopping the lambda manager..."
    sudo kill $(sudo lsof -i -P -n | grep LISTEN | grep 30009 | awk '{print $2}')
}


function log_metrics {
    response=$(curl --no-progress-meter --max-time 60 $LAMBDA_MANAGER_ADDRESS/metrics)
    echo "$response" | grep system_footprint | awk '{print $2}' >> $FOOTPRINT_METRICS_FILENAME
    echo "$response" | grep request_latency_max | awk '{print $2}' >> $MAX_LATENCY_METRICS_FILENAME
    echo "$response" | grep request_latency_avg | awk '{print $2}' >> $AVG_LATENCY_METRICS_FILENAME
}


function start_metrics_scraper {
    alive=true
    while [ "$alive" = "true" ]
    do
        alive=false
        log_metrics &

        sleep 1

        if ps -p $FUNCTION_PID > /dev/null
        then
            alive=true
        fi
    done
}


MODE=$1
DATASET_FILE=$2
ARGO_HOME=$(DIR)/../../../
RUN_HOME=$ARGO_HOME/run/bin
LAMBDA_MANAGER_CONFIG=$ARGO_HOME/run/configs/manager/default-lambda-manager.json
LAMBDA_MANAGER_ADDRESS=localhost:30009
FOOTPRINT_METRICS_FILENAME=footprint.txt
MAX_LATENCY_METRICS_FILENAME=max_latency.txt
AVG_LATENCY_METRICS_FILENAME=avg_latency.txt

if [[ "$MODE" = "gv" ]]; then
    FUNCTION_CODE=$ARGO_HOME/../benchmarks/src/java/gv-sleep/build/libsleep.so
    FUNCTION_NAME=sleepbench
    FUNCTION_ENTRY_POINT=com.sleep.Sleep
    FUNCTION_RUNTIME=graalvisor
elif [[ "$MODE" = "cr" ]]; then
    FUNCTION_CODE=$ARGO_HOME/../benchmarks/src/java/cr-sleep/init.json
    FUNCTION_NAME=crsleepbench
    FUNCTION_ENTRY_POINT=Main
    FUNCTION_RUNTIME=docker.io%2Fopenwhisk%2Fjava8action:latest
else
    echo "Syntax: <gv|cr> /path/to/dataset/directory"
	exit 1
fi


# Deploy lambda manager and wait for it to launch
$RUN_HOME/run deploy lm &
sleep 3

# Configure lambda manager
curl -s -X POST $LAMBDA_MANAGER_ADDRESS/configure_manager -H 'Content-Type: application/json' --data-binary @"$LAMBDA_MANAGER_CONFIG"

echo -n "" > $FOOTPRINT_METRICS_FILENAME
echo -n "" > $MAX_LATENCY_METRICS_FILENAME
echo -n "" > $AVG_LATENCY_METRICS_FILENAME

process_dataset $DATASET_FILE $FUNCTION_CODE $FUNCTION_NAME $FUNCTION_ENTRY_POINT $FUNCTION_RUNTIME &
FUNCTION_PID=$!
start_metrics_scraper &

wait
