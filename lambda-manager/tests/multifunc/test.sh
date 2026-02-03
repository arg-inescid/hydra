#!/bin/bash

# This is a smoke-test for Lambda Manager and HY/OW/KN runtimes for different languages.
# It uploads functions with different runtimes and languages, and performs a single invocation to every registered function.
# The functions in this script are the typical functions from the benchmark suite we use for evaluation.
# NOTE: this script requires the "web" container to be started (see benchmarks/data/start-webserver.sh).

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../benchmarks.sh
source $(DIR)/../shared.sh


function run_hy_benchmarks {
    export FUNCTION_ISOLATION=false
    export INVOCATION_COLLOCATION=true

    for bench in "${HY_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done

    unset FUNCTION_ISOLATION
    unset INVOCATION_COLLOCATION
}

function run_ow_benchmarks {
    for bench in "${OW_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done
}

function run_kn_benchmarks {
    for bench in "${KN_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done
}

function run_gh_benchmarks {
    for bench in "${GH_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done
}

function run {
    export FUNCTION_MEMORY=2048

    start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
    sleep 5

    #run_ow_benchmarks
    #run_kn_benchmarks
    run_gh_benchmarks

    stop_lambda_manager
    unset FUNCTION_MEMORY
}

run
