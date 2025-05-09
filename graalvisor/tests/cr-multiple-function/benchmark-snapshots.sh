#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/shared.sh

# List of benchmarks to run.
BENCH_ARRAY=(jshw jsup jsdh pyhw pyup jvhw jvfh jvhr)
#BENCH_ARRAY=(pyhw pyup jvhw jvfh jvhr)
#BENCH_ARRAY=(jshw jsup jsdh jvhw jvfh jvhr)
#BENCH_ARRAY=(jshw jsup jsdh pyhw pyup)
#BENCH_ARRAY=(jshw pyhw jvhw)
#BENCH_ARRAY=(jvhw jvfh jvhr)
#BENCH_ARRAY=(pyhw pyup)
#BENCH_ARRAY=(jshw jsup jsdh)

function run_benchmark {
    # Clean logs but not snapshots.
    rm -f $(DIR)/{*.log,*.pid}

    # Start hydra.
    start_hydra

    # Upload all functions.
    for bench in "${BENCH_ARRAY[@]}"; do
        upload_function $bench
    done

    # Run ab for each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 1000
    done

    # Run ab for each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 1000
    done

    # Stop hydra.
    stop_hydra
}

run_benchmark
echo "Finished!"