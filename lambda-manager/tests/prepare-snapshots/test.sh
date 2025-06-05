#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

source $(DIR)/../benchmarks.sh
source $(DIR)/../shared.sh

sudo ls &> /dev/null

BENCH_DIR="$ARGO_HOME"/benchmarks/data/apps


function prepare_snapshots {
    export FUNCTION_ISOLATION=true
    export INVOCATION_COLLOCATION=false

    # Register, invoke, and save a snapshot of each function.
    for bench in "${GV_BENCHMARKS[@]}"; do
        # Define bench-specific variables.
        bench_filename=$(basename ${BENCHMARK_CODE["$bench"]})

        sudo rm -f $BENCH_DIR/$bench_filename.memsnap
        sudo rm -f $BENCH_DIR/$bench_filename.metasnap

        register $bench
        request $bench

        # Change the owner of the snapshot files.
        # sudo chown -R $(id -u -n):$(id -g -n) $BENCH_DIR/$bench_filename.memsnap
        # sudo chown -R $(id -u -n):$(id -g -n) $BENCH_DIR/$bench_filename.metasnap
    done

    unset FUNCTION_ISOLATION
    unset INVOCATION_COLLOCATION
}

function run {
    export FUNCTION_MEMORY=2048

    start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
    sleep 5

    prepare_snapshots

    stop_lambda_manager
    unset FUNCTION_MEMORY
}


run
