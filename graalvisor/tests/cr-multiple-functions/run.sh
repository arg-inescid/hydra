#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../cr-single-function/shared.sh

# List of benchmarks to run.
#BENCH_ARRAY=(jshw jsup jsdh pyhw pyup jvhw jvfh jvhr)
# TODO - js uploader seems to break this test.
BENCH_ARRAY=(jshw jsdh pyhw pyup jvhw jvfh jvhr) # No js uploader -> seems to work!

# py + jv (ok)
#BENCH_ARRAY=(pyhw pyup jvhw jvfh jvhr)

# js + jv (ok)
#BENCH_ARRAY=(jshw jsup jsdh jvhw jvfh jvhr)

# js + py (ok)
#BENCH_ARRAY=(jshw jsup jsdh pyhw pyup)

# js (ok)
#BENCH_ARRAY=(jshw pyhw jvhw)

# jv (ok)
#BENCH_ARRAY=(jvhw jvfh jvhr)

# py (ok)
#BENCH_ARRAY=(pyhw pyup)

# ps (ok)
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
        run_ab $bench 1 10000
    done

    # Run ab for each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 10000
    done

    # Stop hydra.
    stop_hydra
}

run_benchmark
echo "Finished!"
