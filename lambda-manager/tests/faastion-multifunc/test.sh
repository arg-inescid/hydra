#!/bin/bash

# This is a smoke-test for Lambda Manager and GV/OW/KN runtimes for different languages.
# It uploads functions with different runtimes and languages, and performs a single invocation to every registered function.
# The functions in this script are the typical functions from the benchmark suite we use for evaluation.
# NOTE: this script requires the "web" container to be started (see benchmarks/data/start-webserver.sh).

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh
source $(DIR)/../faastion-benchmarks.sh
source $(DIR)/../faastion-utils.sh

function run_faastion_benchmarks {
    for bench in "${FAASTION_BENCHMARKS[@]}"; do
        register $bench faastion
        request $bench
    done
}


function run {
    export FUNCTION_MEMORY=2048

    start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
    sleep 5

    run_faastion_benchmarks

    stop_lambda_manager
    unset FUNCTION_MEMORY
}

run
