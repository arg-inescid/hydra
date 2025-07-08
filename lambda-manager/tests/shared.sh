#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

# Load benchmarks configs.
source "$(dirname "${BASH_SOURCE[0]}")/benchmarks.sh"


GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

LAMBDA_MANAGER_HOST=localhost
LAMBDA_MANAGER_PORT=30008
LAMBDA_MANAGER_SOCKET_PORT=30009
LAMBDA_MANAGER_HOME=$ARGO_HOME/lambda-manager

USER=user


function register {
    bench=$1
    mode=${bench:0:2}
    register_func="register_$mode"

    # Invoke mode-specific function.
    "$register_func" "$bench"
}

function register_gv {
    bench=$1

    if [ -z "$FUNCTION_MEMORY" ]; then
        FUNCTION_MEMORY=512
    fi
    if [ -z "$FUNCTION_ISOLATION" ]; then
        FUNCTION_ISOLATION=false
    fi
    if [ -z "$INVOCATION_COLLOCATION" ]; then
        INVOCATION_COLLOCATION=true
    fi

    lang=
    if [[ $bench == "gv_jv"* ]]; then
        lang=java
    elif [[ $bench == "gv_py"* ]]; then
        lang=python
    elif [[ $bench == "gv_js"* ]]; then
        lang=javascript
    else
        echo "Cannot determine language of the benchmark: $bench"
    fi

    runtime=graalvisor

    entrypoint=${BENCHMARK_ENTRYPOINTS["$bench"]}
    code=${BENCHMARK_CODE["$bench"]}
    sandbox=${BENCHMARK_SANDBOXES["$bench"]}
    svm_id=${BENCHMARK_SVMIDS["$bench"]}

    gv_parameters="gv_sandbox=$sandbox"
    if [ -n "$svm_id" ]; then
        gv_parameters="$gv_parameters&svm_id=$svm_id"
    fi

    curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=$USER\&function_name=$bench\&function_language=$lang\&function_entry_point=$entrypoint\&function_memory=$FUNCTION_MEMORY\&function_runtime=$runtime\&function_isolation=$FUNCTION_ISOLATION\&invocation_collocation=$INVOCATION_COLLOCATION\&$gv_parameters -H 'Content-Type: text/plain' --data $code
}

function register_ow {
    bench=$1

    if [ -z "$FUNCTION_MEMORY" ]; then
        FUNCTION_MEMORY=512
    fi
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=false

    runtime=openwhisk
    lang=
    if [[ $bench == *"_jv_"* ]]; then
        lang=java
    elif [[ $bench == *"_js_"* ]]; then
        lang=javascript
    elif [[ $bench == *"_py_"* ]]; then
        lang=python
    else
        echo "Unknown benchmark language: $bench"
        exit 1
    fi

    entrypoint=${BENCHMARK_ENTRYPOINTS["$bench"]}
    code=${BENCHMARK_CODE["$bench"]}

    curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=$USER\&function_name=$bench\&function_language=$lang\&function_entry_point=$entrypoint\&function_memory=$FUNCTION_MEMORY\&function_runtime=$runtime\&function_isolation=$FUNCTION_ISOLATION\&invocation_collocation=$INVOCATION_COLLOCATION -H 'Content-Type: text/plain' --data $code
}

function register_kn {
    bench=$1

    if [ -z "$FUNCTION_MEMORY" ]; then
        FUNCTION_MEMORY=512
    fi
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=true

    runtime=knative
    lang=
    if [[ $bench == *"_jv_"* ]]; then
        lang=java
    elif [[ $bench == *"_js_"* ]]; then
        lang=javascript
    elif [[ $bench == *"_py_"* ]]; then
        lang=python
    else
        echo "Unknown benchmark language: $bench"
        exit 1
    fi

    entrypoint=${BENCHMARK_ENTRYPOINTS["$bench"]}
    code=${BENCHMARK_CODE["$bench"]}

    curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=$USER\&function_name=$bench\&function_language=$lang\&function_entry_point=$entrypoint\&function_memory=$FUNCTION_MEMORY\&function_runtime=$runtime\&function_isolation=$FUNCTION_ISOLATION\&invocation_collocation=$INVOCATION_COLLOCATION -H 'Content-Type: text/plain' --data $code
}

function request {
    bench=$1

    echo -e "${GREEN}Invoking $bench...${NC}"
    curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/$USER/$bench -H 'Content-Type: application/json' --data ${BENCHMARK_PAYLOADS["$bench"]}
}

function benchmark {
    bench=$1

    payload=${BENCHMARK_PAYLOADS["$bench"]}

    if [ -z "$RESULTS_DIR" ]; then
        echo -e "${RED}Define RESULTS_DIR.${NC}"
    fi

    if [ -z "$CONCURRENCY" ]; then
        CONCURRENCY=1
    fi
    if [ -z "$WORKLOAD" ]; then
        WORKLOAD=500
    fi

    app_post=/tmp/app-post
    echo $payload > $app_post
    results_file=$RESULTS_DIR/"$USER-$bench.log"

    echo -e "${GREEN}Benchmarking $bench...${NC}"
    ab -p $app_post -T application/json -c $CONCURRENCY -n $WORKLOAD http://$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/$USER/$bench &> $results_file

    res=$(cat $results_file | grep "Time per request" | grep "(mean)")
    echo -e "${GREEN}Mean latency for $bench:\n$res${NC}"

    rm $app_post
}

function wait_port {
    host=$1
    port=$2
    while ! nc -z $host $port; do sleep 0.01; done
}

function start_lambda_manager {
    config_path=$1
    variables_path=$2

    bash $LAMBDA_MANAGER_HOME/deploy.sh --config $config_path --variables $variables_path --http &

    wait_port $LAMBDA_MANAGER_HOST $LAMBDA_MANAGER_PORT
}

function stop_lambda_manager {
    kill $(lsof -i -P -n | grep LISTEN | grep $LAMBDA_MANAGER_PORT | awk '{print $2}')
}

function start_lambda_manager_socket {
    config_path=$1
    variables_path=$2

    bash $LAMBDA_MANAGER_HOME/deploy.sh --config $config_path --variables $variables_path --socket &

    wait_port $LAMBDA_MANAGER_HOST $LAMBDA_MANAGER_SOCKET_PORT
}

function stop_lambda_manager_socket {
    kill $(lsof -i -P -n | grep LISTEN | grep $LAMBDA_MANAGER_SOCKET_PORT | awk '{print $2}')
}
