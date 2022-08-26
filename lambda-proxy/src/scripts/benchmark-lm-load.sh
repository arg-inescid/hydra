#!/bin/bash

# Example usage of this script:
# bash benchmark-lm-load.sh /path/to/dataset/directory

# The dataset directory should contain the .csv files, each file
# represents the list of all invocations for one particular owner.
# The structure of each .csv file should be as follows:
# HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp CurrentConcurrency CurrentMemoryUsage
# Important note: all .csv files should be named as follows:
# result_owner_{owner-name}.csv

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}


function process_owner {
    csv_file=$1
    function_code=$2
    owner=${csv_file%.*}
    owner=${owner##*_}

    # Upload function for current owner
    curl -s -X POST $LAMBDA_MANAGER_ADDRESS/upload_function?username=$owner\&function_name=sleepbench\&function_language=java\&function_entry_point=com.sleep.Sleep\&function_memory=150\&function_runtime=graalvisor \
        -H 'Content-Type: application/octet-stream' --data-binary @"$function_code"

    current_timestamp=0
    tail -n +2 $csv_file |
    while IFS=, read -r HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp CurrentConcurrency CurrentMemoryUsage
    do
        time_to_sleep=$(python3 -c "print((($Timestamp - $current_timestamp) % 3600000) / 1000)")
        current_timestamp=$Timestamp
        sleep $time_to_sleep
        curl -s -X POST $LAMBDA_MANAGER_ADDRESS/$owner/sleepbench -H 'Content-Type: application/json' --data '{"memory":"'$AverageAllocatedMb'","sleep":"'$AverageDuration'"}' &
    done
}


DATASET_DIR=$1
ARGO_HOME=$(DIR)/../../../
RUN_HOME=$ARGO_HOME/run/bin
LAMBDA_MANAGER_CONFIG=$ARGO_HOME/run/configs/manager/default-lambda-manager.json
LAMBDA_MANAGER_ADDRESS=localhost:30009
FUNCTION_CODE=$ARGO_HOME/../benchmarks/src/java/gv-sleep/build/libsleep.so

# Deploy lambda manager and wait for it to launch
$RUN_HOME/run deploy lm &
sleep 3

# Configure lambda manager
curl -s -X POST $LAMBDA_MANAGER_ADDRESS/configure_manager -H 'Content-Type: application/json' --data-binary @"$LAMBDA_MANAGER_CONFIG"

for dataset_file in $DATASET_DIR/result_owner_*
do
    process_owner $dataset_file $FUNCTION_CODE &
done

echo "Finished benchmark execution."

wait
