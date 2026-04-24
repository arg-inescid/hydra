#!/bin/bash

# This is a smoke-test for Lambda Manager and HY/OW/KN runtimes for different languages.
# It uploads functions with different runtimes and languages, and performs a single invocation to every registered function.
# The functions in this script are the typical functions from the benchmark suite we use for evaluation.
# NOTE: this script requires the "web" container to be started (see benchmarks/data/start-webserver.sh).

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

if [[ -z "${GRAALOS_SEBS}" ]]; then
    echo "GRAALOS_SEBS is not defined. Exiting..."
    exit 1
fi

source $(DIR)/../benchmarks.sh
source $(DIR)/../shared.sh

function test_ow_benchmarks {
    start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
    sleep 5
    for bench in "${OW_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done
    stop_lambda_manager
}

function test_kn_benchmarks {
    start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
    sleep 5
    for bench in "${KN_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done
    stop_lambda_manager
}

function test_gh_benchmarks {
    start_lambda_manager $(DIR)/config-gh.json $(DIR)/variables.json
    sleep 5
    for bench in "${GH_BENCHMARKS[@]}"; do
	register $bench
        request $bench
    done
    stop_lambda_manager
}

function run_ow_benchmarks {
    start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
    sleep 5
    for bench in "${OW_BENCHMARKS[@]}"; do
        register $bench
        benchmark $bench
    done    
    stop_lambda_manager
}

function run_kn_benchmarks {
    start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
    sleep 5
    for bench in "${KN_BENCHMARKS[@]}"; do
        register $bench
        benchmark $bench
    done
    stop_lambda_manager
}

function run_gh_benchmarks {
    start_lambda_manager $(DIR)/config-gh.json $(DIR)/variables.json
    sleep 5
    for bench in "${GH_BENCHMARKS[@]}"; do
	register $bench
        benchmark $bench
    done
    stop_lambda_manager
}

function test_all {
    test_ow_benchmarks
    sleep 5

    test_kn_benchmarks
    sleep 5

    test_gh_benchmarks
    sleep 5
}

function run_all {
    for conc in 1 4 16 64
    do
        export CONCURRENCY=$conc
        export WORKLOAD=$(echo "$conc * 1000" | bc)
        run_ow_benchmarks
        sleep 5
        run_kn_benchmarks
        sleep 5
        run_gh_benchmarks
        sleep 5
    done
}

# Default values for running benchmarks.
export WORKLOAD=1000
export CONCURRENCY=8
export FUNCTION_MEMORY=2048
export RESULTS_DIR=$(pwd)

#test_ow_benchmarks
#test_kn_benchmarks
#test_gh_benchmarks
test_all

#run_ow_benchmarks
#run_kn_benchmarks
#run_gh_benchmarks
#run_all
    
unset FUNCTION_MEMORY
