#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/shared.sh

BENCH_ARRAY=(jshw jsup jsdh pyhw pyup jvhw jvfh jvhr)

# Ensure that we have a clean environment to start.
bash $(DIR)/cleanup.sh

# For each benchmark.
for bench in "${BENCH_ARRAY[@]}"; do
    # Start hydra.
    start_hydra

    # Upload the function.
    upload_function $bench

    # Create the snapshot after 1 request.
    run_ab $bench 1 1000

    # Stop hydra.
    stop_hydra

    # Wait for all subprocesses to terminate (hydra in particular).
    wait
done