#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

# Load benchmarks configs.
source "$(dirname "${BASH_SOURCE[0]}")/faastion-benchmarks.sh"


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
  runtime=$2

  if [ -z "$FUNCTION_MEMORY" ]; then
    FUNCTION_MEMORY=512
  fi

  function_isolation=true
  invocation_collocation=true
  lang=java

  code=
  if [[ $runtime == "faastion" ]]; then
    code=${BENCHMARK_CODE["$bench"]}
  elif [[ $runtime == "faastlane" ]]; then
    code=${BENCHMARK_CODE_VANILLA["$bench"]}
  elif [[ $runtime == "faastion-lpi" ]]; then
    code="${BENCHMARK_CODE["$bench"]}:${BENCHMARK_CODE_VANILLA["$bench"]}"
  else
    echo "Cannot determine faastion runtime: $runtime. The second parameter should be 'faastion', 'faastlane', or 'faastion-lpi'."
  fi

  entrypoint=${BENCHMARK_ENTRYPOINTS["$bench"]}
  bench_id=${bench:(-2)}

  curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=$USER\&function_name=$bench\&function_language=$lang\&function_entry_point=$entrypoint\&function_memory=$FUNCTION_MEMORY\&function_runtime=$runtime\&function_isolation=$function_isolation\&invocation_collocation=$invocation_collocation\&benchmark_name=$bench_id -H 'Content-Type: text/plain' --data $code
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
