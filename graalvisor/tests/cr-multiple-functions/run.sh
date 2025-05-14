#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../cr-single-function/shared.sh

# List of benchmarks to run.
# TODO - js uploader seems to break when co-located with other benchmarks.
BENCH_ARRAY=(jshw jsdh pyhw pymst pybfs pypr pydna pydh pyco pytn pyup jvhw jvfh jvcy jvhr)

# py + jv (ok)
#BENCH_ARRAY=(pyhw pymst pybfs pypr pydna pydh pyco pytn pyup jvhw jvfh jvcy jvhr)

# js + jv (ok)
#BENCH_ARRAY=(jshw jsdh jvhw jvfh jvcy jvhr)

# js + py (ok)
#BENCH_ARRAY=(jshw jsup jsdh pyhw pymst pybfs pypr pydna pydh pyco pytn pyup)

# jv (ok)
#BENCH_ARRAY=(jvhw jvfh jvcy jvhr)

# py (ok)
#BENCH_ARRAY=(pyhw pymst pybfs pypr pydna pydh pyco pytn pyup)

# ps (ok)
#BENCH_ARRAY=(jshw jsup jsdh)

function run_benchmark {
    # Clean logs but not snapshots.
    bash $(DIR)/cleanup.sh

    # Start hydra.
    start_hydra

    # Upload all functions.
    for bench in "${BENCH_ARRAY[@]}"; do
        upload_function $bench
    done

    # Run ab for each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 100
    done

    # Run ab for each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 100
    done

    # Stop hydra.
    stop_hydra
}

run_benchmark
echo "Finished!"
