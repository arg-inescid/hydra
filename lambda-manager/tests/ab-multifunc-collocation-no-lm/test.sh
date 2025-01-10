#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

HYDRA_ADDRESS="localhost:8080"
CONTAINER_NAME="lambda_test"

CONCURRENCY=1
REQUESTS=100

BENCH_ARRAY=(jshw jsup jsdh pyhw pyup pyco jvhw jvfh jvhr)

declare -A BENCHMARK_REGISTER_QUERY
BENCHMARK_REGISTER_QUERY[jshw]="name=jshw&language=java&entrypoint=com.helloworld.HelloWorld&isBinary=true&svmid=1&sandbox=snapshot&url=http://172.18.0.1:8000/apps/gv-js-hello-world.so"
BENCHMARK_REGISTER_QUERY[jsup]="name=jsup&language=java&entrypoint=com.uploader.Uploader&isBinary=true&svmid=2&sandbox=snapshot&url=http://172.18.0.1:8000/apps/gv-js-uploader.so"
BENCHMARK_REGISTER_QUERY[jsdh]="name=jsdh&language=java&entrypoint=com.dynamichtml.DynamicHTML&isBinary=true&svmid=3&sandbox=snapshot&url=http://172.18.0.1:8000/apps/gv-js-dynamic-html.so"
BENCHMARK_REGISTER_QUERY[pyhw]="name=pyhw&language=java&entrypoint=com.helloworld.HelloWorld&isBinary=true&svmid=4&sandbox=snapshot&url=http://172.18.0.1:8000/apps/gv-py-hello-world.so"
BENCHMARK_REGISTER_QUERY[pyup]="name=pyup&language=java&entrypoint=com.uploader.Uploader&isBinary=true&svmid=5&sandbox=snapshot&url=http://172.18.0.1:8000/apps/gv-py-uploader.so"
BENCHMARK_REGISTER_QUERY[pyco]="name=pyco&language=java&entrypoint=com.compression.Compression&isBinary=true&svmid=6&sandbox=snapshot&url=http://172.18.0.1:8000/apps/gv-py-compression.so"
BENCHMARK_REGISTER_QUERY[jvhw]="name=jvhw&language=java&entrypoint=com.hello_world.HelloWorld&isBinary=true&sandbox=isolate&url=http://172.18.0.1:8000/apps/gv-jv-hello-world.so"
BENCHMARK_REGISTER_QUERY[jvfh]="name=jvfh&language=java&entrypoint=com.filehashing.FileHashing&isBinary=true&sandbox=isolate&url=http://172.18.0.1:8000/apps/gv-jv-file-hashing.so"
BENCHMARK_REGISTER_QUERY[jvhr]="name=jvhr&language=java&entrypoint=com.httprequest.HttpRequest&isBinary=true&sandbox=isolate&url=http://172.18.0.1:8000/apps/gv-jv-httprequest.so"

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

function start_container {
    docker run --privileged --rm --name="$CONTAINER_NAME" \
        -e app_dir=/codebase/ \
        -p 8080:8080 \
        -v "$ARGO_HOME"/benchmarks/data/apps:/codebase \
        graalvisor:latest &> $(DIR)/lambda.log &
}

function start_native {
    app_dir=/tmp/apps "$ARGO_HOME"/graalvisor/build/native-image/polyglot-proxy &> $(DIR)/lambda.log &
    PID="$!"
}

function stop_container {
    docker container stop "$CONTAINER_NAME"
}

function stop_native {
    kill $PID
}


function upload_function {
    bench=$1
    curl -X POST $HYDRA_ADDRESS/register?${BENCHMARK_REGISTER_QUERY["$bench"]}
}

function run_ab {
    bench=$1

    APP_POST="/tmp/app-post-$bench"
    echo '{"arguments":"{ \"url\":\"http://172.18.0.1:8000/snap.png\" }","name":"'$bench'"}' > $APP_POST
    ab_log_file="$bench-ab.log"

    ab -p $APP_POST -T application/json -c $CONCURRENCY -n $REQUESTS http://$HYDRA_ADDRESS/${BENCHMARK_RUN_ENDPOINT["$bench"]} &> $ab_log_file

    rm $APP_POST
}

function run_request {
    bench=$1
    res=$(curl -s -X POST $HYDRA_ADDRESS/${BENCHMARK_RUN_ENDPOINT["$bench"]} --data '{"arguments":"{ \"url\":\"http://172.18.0.1:8000/snap.png\" }","name":"'$bench'"}')
    echo -e "Result:\n$res\n"
}


# Start Hydra.
start_container
sleep 5

for bench in "${BENCH_ARRAY[@]}"; do
    upload_function $bench
    run_ab $bench &
    # run_request $bench
done

wait

# Stop Hydra.
stop_container

echo "Finished!"
