#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

HYDRA_HOST="localhost"
HYDRA_PORT="8080"
HYDRA_ADDRESS="$HYDRA_HOST:$HYDRA_PORT"

# How to use svm snapshots and avoid some of the current limitations:
# - pyco is a problematic benchmark (needs malloc virtualization), replace w/ py/pr, pybfs, py/dna, etc;
# - need to generate snapshots one by one;
# - when generating snapshots, issue many requests so that the heap size stabilizies;
BENCH_ARRAY=(jshw jsup jsdh pyhw pyup jvhw jvfh jvhr)
#BENCH_ARRAY=(pyhw pyup jvhw jvfh jvhr)
#BENCH_ARRAY=(jshw jsup jsdh jvhw jvfh jvhr)
#BENCH_ARRAY=(jshw jsup jsdh pyhw pyup)
#BENCH_ARRAY=(jshw pyhw jvhw)
#BENCH_ARRAY=(jvhw jvfh jvhr)
#BENCH_ARRAY=(pyhw pyup)
#BENCH_ARRAY=(jshw jsup jsdh)

declare -A BENCHMARK_REGISTER_QUERY
BENCHMARK_REGISTER_QUERY[jshw]="name=jshw&language=java&entrypoint=com.helloworld.HelloWorld&isBinary=true&svmid=1&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-hello-world.so"
BENCHMARK_REGISTER_QUERY[jsup]="name=jsup&language=java&entrypoint=com.uploader.Uploader&isBinary=true&svmid=2&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-uploader.so"
BENCHMARK_REGISTER_QUERY[jsdh]="name=jsdh&language=java&entrypoint=com.dynamichtml.DynamicHTML&isBinary=true&svmid=3&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-dynamic-html.so"
BENCHMARK_REGISTER_QUERY[pyhw]="name=pyhw&language=java&entrypoint=com.helloworld.HelloWorld&isBinary=true&svmid=4&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-hello-world.so"
BENCHMARK_REGISTER_QUERY[pyup]="name=pyup&language=java&entrypoint=com.uploader.Uploader&isBinary=true&svmid=5&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-uploader.so"
BENCHMARK_REGISTER_QUERY[pyco]="name=pyco&language=java&entrypoint=com.compression.Compression&isBinary=true&svmid=6&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-compression.so"
BENCHMARK_REGISTER_QUERY[jvhw]="name=jvhw&language=java&entrypoint=com.hello_world.HelloWorld&isBinary=true&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-hello-world.so"
BENCHMARK_REGISTER_QUERY[jvfh]="name=jvfh&language=java&entrypoint=com.filehashing.FileHashing&isBinary=true&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-file-hashing.so"
BENCHMARK_REGISTER_QUERY[jvhr]="name=jvhr&language=java&entrypoint=com.httprequest.HttpRequest&isBinary=true&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-httprequest.so"

declare -A BENCHMARK_RUN_ENDPOINT
BENCHMARK_RUN_ENDPOINT[jshw]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[jsup]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[jsdh]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pyhw]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pyup]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pyco]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[jvhw]=""
BENCHMARK_RUN_ENDPOINT[jvfh]=""
BENCHMARK_RUN_ENDPOINT[jvhr]=""

function start_hydra {
    export app_dir=$(DIR)
    bash $(DIR)/../../../graalvisor/graalvisor $(DIR)/graalvisor.pid &>> graalvisor.log &

    # Wait for hydra to launch.
    timeout 1s bash -c "while ! nc -z $HYDRA_HOST $HYDRA_PORT; do sleep 0.1; done"

    # TODO - after 1 second, if the port is not open, give up with an error.
}

function stop_hydra {
    # Note: wait until pid file is filled.
    timeout 1s bash -c "while [ ! -s $(DIR)/hydra.pid ]; do sleep 0.1; done"

    # Kill hydra.
    if [ -f graalvisor.pid ]; then
        echo "killing graalvisor running with pid $(cat $(DIR)/graalvisor.pid)"
        kill $(cat $(DIR)/graalvisor.pid)
        rm $(DIR)/graalvisor.pid
    else
        echo "error: graalvisor.pid not found."
    fi
}

function upload_function {
    bench=$1
    curl -X POST $HYDRA_ADDRESS/register?${BENCHMARK_REGISTER_QUERY["$bench"]}
    echo ""
}

function run_ab {
    bench=$1
    conc=$2
    reqs=$3

    APP_POST="/tmp/app-post-$bench"
    echo '{"arguments":"{ \"url\":\"http://127.0.0.1:8000/snap.png\" }","name":"'$bench'"}' > $APP_POST

    ab -p $APP_POST -T application/json -c $conc -n $reqs http://$HYDRA_ADDRESS/${BENCHMARK_RUN_ENDPOINT["$bench"]} &> $bench-ab.log
    rm $APP_POST
    echo "Ran function $bench"
}

function prepare_snapshots {
    # Ensure a clean setup (no previous snapshots).
    bash $(DIR)/cleanup.sh

    # For each benchmark.
    for bench in "${BENCH_ARRAY[@]}"; do
        # Start hydra.
        start_hydra

        # Upload the function.
        upload_function $bench

        # Create the snapshot after 1 request.
        run_ab $bench 1 1

        # Stop hydra.
        stop_hydra

        # Wait for all subprocesses to terminate (hydra in particular).
        wait
    done
}

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
        run_ab $bench 1 100
    done

    # Run ab for each function.
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 100
    done

    # Stop hydra.
    stop_hydra
}

echo "Generating snapshots:"
prepare_snapshots

echo "Running tests:"
run_benchmark

echo "Finished!"

echo "Dumping graalvisor log (eleminating duplicates and removing the request prints):"
cat $(DIR)/graalvisor.log | grep -v took | awk '!seen[$0]++'
