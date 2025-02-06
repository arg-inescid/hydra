#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

HYDRA_ADDRESS="localhost:8080"
CONTAINER_NAME="lambda_test"

# How to use svm snapshots and avoid some of the current limitations:
# - pyco is a problematic benchmark (needs malloc virtualization), replace w/ py/pr, pybfs, py/dna, etc;
# - need to use setarch -R;
# - need to generate snapshots one by one;
# - when generating snapshots, issue many requests so that the heap size stabilizies;
#BENCH_ARRAY=(jshw jsup jsdh pyhw pyup jvhw jvfh jvhr)
BENCH_ARRAY=(jshw jsup jsdh pyhw pyup jvhw jvfh)

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
    bench=$1
    export app_dir=$(DIR)/apps
    setarch -R  $ARGO_HOME/graalvisor/build/native-image/polyglot-proxy -Xmx32g &> $(DIR)/$bench-hydra.log &
    echo $! > $(DIR)/hydra.pid
    sleep 1
    #sudo strace -o $(DIR)/$bench-hydra.strace -p $(cat $(DIR)/hydra.pid) -f &
    echo "Started hydra with pid $(cat $(DIR)/hydra.pid)"
}

function stop_hydra {
    kill $(cat $(DIR)/hydra.pid)
    echo "Stopped hydra with pid $(cat $(DIR)/hydra.pid)"
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
    ab_log_file="$bench-ab.log"

    ab -p $APP_POST -T application/json -c $conc -n $reqs http://$HYDRA_ADDRESS/${BENCHMARK_RUN_ENDPOINT["$bench"]} &> $ab_log_file
    rm $APP_POST
    echo "Ran function $bench"
}

function prepare_snapshots {
    for bench in "${BENCH_ARRAY[@]}"; do
        start_hydra $bench
        upload_function $bench
        run_ab $bench 1 1000
        stop_hydra
    done
}

function run_benchmark {
    start_hydra "run"
    for bench in "${BENCH_ARRAY[@]}"; do
        upload_function $bench
    done
    for bench in "${BENCH_ARRAY[@]}"; do
        run_ab $bench 1 100 # TODO - test w/ 2k.
    done
    stop_hydra
}

# Clear apps and logs
rm -rf $(DIR)/apps $(DIR)/*.log $(DIR)/*.pid
mkdir -p $(DIR)/apps
prepare_snapshots
run_benchmark

echo "Finished!"
