#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

source $(DIR)/../shared.sh

sudo ls &> /dev/null

# Declare global variables.
USERNAME=user
BENCH_ARRAY=(jshw jsup jsdh pyhw pyup pyco)
BENCH_DIR="$ARGO_HOME"/benchmarks/data/apps

FUNCTION_MEMORY=1024
FUNCTION_ISOLATION=true
INVOCATION_COLLOCATION=false

declare -A BENCHMARK_BINARIES
BENCHMARK_BINARIES[jshw]="http://172.18.0.1:8000/apps/gv-js-hello-world.so"
BENCHMARK_BINARIES[jsup]="http://172.18.0.1:8000/apps/gv-js-uploader.so"
BENCHMARK_BINARIES[jsdh]="http://172.18.0.1:8000/apps/gv-js-dynamic-html.so"
BENCHMARK_BINARIES[pyhw]="http://172.18.0.1:8000/apps/gv-py-hello-world.so"
BENCHMARK_BINARIES[pyup]="http://172.18.0.1:8000/apps/gv-py-uploader.so"
BENCHMARK_BINARIES[pyco]="http://172.18.0.1:8000/apps/gv-py-compression.so"

declare -A BENCHMARK_ENTRYPOINTS
BENCHMARK_ENTRYPOINTS[jshw]="com.helloworld.HelloWorld"
BENCHMARK_ENTRYPOINTS[jsup]="com.uploader.Uploader"
BENCHMARK_ENTRYPOINTS[jsdh]="com.dynamichtml.DynamicHTML"
BENCHMARK_ENTRYPOINTS[pyhw]="com.helloworld.HelloWorld"
BENCHMARK_ENTRYPOINTS[pyup]="com.uploader.Uploader"
BENCHMARK_ENTRYPOINTS[pyco]="com.compression.Compression"

declare -A BENCHMARK_PAYLOADS
BENCHMARK_PAYLOADS[jshw]='{}'
BENCHMARK_PAYLOADS[jsup]='{"url":"http://172.18.0.1:8000/snap.png"}'
BENCHMARK_PAYLOADS[jsdh]='{"url":"http://172.18.0.1:8000/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[pyhw]='{}'
BENCHMARK_PAYLOADS[pyup]='{"url":"http://172.18.0.1:8000/snap.png"}'
BENCHMARK_PAYLOADS[pyco]='{"url":"http://172.18.0.1:8000/video.mp4"}'

declare -A BENCHMARK_SVMIDS
BENCHMARK_SVMIDS[jshw]="1"
BENCHMARK_SVMIDS[jsup]="2"
BENCHMARK_SVMIDS[jsdh]="3"
BENCHMARK_SVMIDS[pyhw]="4"
BENCHMARK_SVMIDS[pyup]="5"
BENCHMARK_SVMIDS[pyco]="6"

start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
sleep 5

# Register, invoke, and save a snapshot of each function.
for bench in "${BENCH_ARRAY[@]}"; do
    # Define bench-specific variables.
    bench_filename=$(basename ${BENCHMARK_BINARIES["$bench"]})

    rm -f $BENCH_DIR/$bench_filename.memsnap
    rm -f $BENCH_DIR/$bench_filename.metasnap

    # Register.
    curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=$USERNAME\&function_name=$bench\&function_language=java\&function_entry_point=${BENCHMARK_ENTRYPOINTS["$bench"]}\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=$FUNCTION_ISOLATION\&invocation_collocation=$INVOCATION_COLLOCATION\&gv_sandbox=context-snapshot\&svm_id=${BENCHMARK_SVMIDS["$bench"]} -H 'Content-Type: text/plain' --data ${BENCHMARK_BINARIES["$bench"]}
    # Invoke.
    curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/$USERNAME/$bench -H 'Content-Type: application/json' --data ${BENCHMARK_PAYLOADS["$bench"]}

    # Change the owner of the snapshot files.
    sudo chown -R $(id -u -n):$(id -g -n) $BENCH_DIR/$bench_filename.memsnap
    sudo chown -R $(id -u -n):$(id -g -n) $BENCH_DIR/$bench_filename.metasnap
done

stop_lambda_manager
