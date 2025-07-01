#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/shared.sh

BENCH_ARRAY=(pymst pyhw)

BENCHMARK_REGISTER_QUERY[pyhw]="name=pyhw&language=java&entrypoint=com.helloworld.HelloWorld&svmid=5&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-hello-world.so"
BENCHMARK_REGISTER_QUERY[pymst]="name=pymst&language=java&entrypoint=com.mst.MST&svmid=6&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-mst.so"

function run_benchmark {
    # Clean logs but not snapshots.
    bash $(DIR)/cleanup.sh

    # Start hydra.
    start_hydra

    # Upload all functions.
    for bench in "${BENCH_ARRAY[@]}"; do
        upload_function $bench
    done

    # Restore each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 1
    done

    # Run ab for each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 5 100 &
        echo $! > $bench-ab.pid
    done

    # Wait for all ab instances to finish.
    for bench in "${BENCH_ARRAY[@]}"; do
        wait $(cat $bench-ab.pid)
    done

    # Stop hydra.
    stop_hydra
}

run_benchmark
echo "Finished!"
