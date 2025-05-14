#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

HYDRA_HOST="localhost"
HYDRA_PORT="8080"
HYDRA_ADDRESS="$HYDRA_HOST:$HYDRA_PORT"

# How to use svm snapshots and avoid some of the current limitations:
# - need to generate snapshots one by one;
# - when generating snapshots, issue many requests so that the heap size stabilizies;

declare -A BENCHMARK_REGISTER_QUERY
BENCHMARK_REGISTER_QUERY[jvhw]="name=jvhw&language=java&entrypoint=com.hello_world.HelloWorld&isBinary=true&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-hello-world.so"
BENCHMARK_REGISTER_QUERY[jvfh]="name=jvfh&language=java&entrypoint=com.filehashing.FileHashing&isBinary=true&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-file-hashing.so"
BENCHMARK_REGISTER_QUERY[jvcy]="name=jvcy&language=java&entrypoint=com.classify.Classify&isBinary=true&sandbox=process&url=http://127.0.0.1:8000/apps/gv-jv-classify.zip"
BENCHMARK_REGISTER_QUERY[jvhr]="name=jvhr&language=java&entrypoint=com.httprequest.HttpRequest&isBinary=true&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-httprequest.so"

BENCHMARK_REGISTER_QUERY[jshw]="name=jshw&language=java&entrypoint=com.helloworld.HelloWorld&isBinary=true&svmid=1&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-hello-world.so"
BENCHMARK_REGISTER_QUERY[jsdh]="name=jsdh&language=java&entrypoint=com.dynamichtml.DynamicHTML&isBinary=true&svmid=2&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-dynamic-html.so"
BENCHMARK_REGISTER_QUERY[jsup]="name=jsup&language=java&entrypoint=com.uploader.Uploader&isBinary=true&svmid=4&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-uploader.so"

BENCHMARK_REGISTER_QUERY[pyhw]="name=pyhw&language=java&entrypoint=com.helloworld.HelloWorld&isBinary=true&svmid=5&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-hello-world.so"
BENCHMARK_REGISTER_QUERY[pymst]="name=pymst&language=java&entrypoint=com.mst.MST&isBinary=true&svmid=6&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-mst.so"
BENCHMARK_REGISTER_QUERY[pybfs]="name=pybfs&language=java&entrypoint=com.bfs.BFS&isBinary=true&svmid=7&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-bfs.so"
BENCHMARK_REGISTER_QUERY[pypr]="name=pypr&language=java&entrypoint=com.pr.PageRank&isBinary=true&svmid=8&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-pagerank.so"
BENCHMARK_REGISTER_QUERY[pydna]="name=pydna&language=java&entrypoint=com.dna.DNA&isBinary=true&svmid=9&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-dna.so"
BENCHMARK_REGISTER_QUERY[pydh]="name=pydh&language=java&entrypoint=com.dynamichtml.DynamicHTML&isBinary=true&svmid=10&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-dynamic-html.so"
BENCHMARK_REGISTER_QUERY[pyco]="name=pyco&language=java&entrypoint=com.compression.Compression&isBinary=true&svmid=11&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-compression.so"
BENCHMARK_REGISTER_QUERY[pytn]="name=pytn&language=java&entrypoint=com.thumbnail.Thumbnail&isBinary=true&svmid=12&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-thumbnail.so"
BENCHMARK_REGISTER_QUERY[pyup]="name=pyup&language=java&entrypoint=com.uploader.Uploader&isBinary=true&svmid=13&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-uploader.so"

declare -A BENCHMARK_RUN_ENDPOINT
BENCHMARK_RUN_ENDPOINT[jvhw]=""
BENCHMARK_RUN_ENDPOINT[jvfh]=""
BENCHMARK_RUN_ENDPOINT[jvcy]=""
BENCHMARK_RUN_ENDPOINT[jvhr]=""

BENCHMARK_RUN_ENDPOINT[jshw]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[jsdh]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[jsup]="warmup?concurrency=1&requests=1"

BENCHMARK_RUN_ENDPOINT[pyhw]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pymst]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pybfs]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pypr]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pydna]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pydh]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pyco]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pytn]="warmup?concurrency=1&requests=1"
BENCHMARK_RUN_ENDPOINT[pyup]="warmup?concurrency=1&requests=1"

declare -A BENCHMARK_POST
BENCHMARK_POST[jvhw]='{"arguments":"","name":"jvhw"}'
BENCHMARK_POST[jvfh]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/snap.png\" }","name":"jvfh"}'
BENCHMARK_POST[jvhr]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/snap.png\" }","name":"jvhr"}'
BENCHMARK_POST[jvcy]='{"arguments":"{ \"model_url\":\"http://127.0.0.1:8000/tensorflow_inception_graph.pb\", \"labels_url\": \"http://127.0.0.1:8000/imagenet_comp_graph_label_strings.txt\", \"image_url\":\"http://127.0.0.1:8000/eagle.jpg\" }","name":"jvcy"}'

BENCHMARK_POST[jshw]='{"arguments":"","name":"jshw"}'
BENCHMARK_POST[jsdh]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/template.html\", \"username\":\"rbruno\", \"nsize\":\"10\" }","name":"jsdh"}'
BENCHMARK_POST[jsup]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/snap.png\" }","name":"jsup"}'

BENCHMARK_POST[pyhw]='{"arguments":"","name":"pyhw"}'
BENCHMARK_POST[pymst]='{"arguments":"{ \"size\":\"100\" }","name":"pymst"}'
BENCHMARK_POST[pybfs]='{"arguments":"{ \"size\":\"100\" }","name":"pybfs"}'
BENCHMARK_POST[pypr]='{"arguments":"{ \"size\":\"10\" }","name":"pypr"}'
BENCHMARK_POST[pydna]='{"arguments":"{ \"fasta_url\":\"http://127.0.0.1:8000/bacillus_subtilis.fasta\" }","name":"pydna"}'
BENCHMARK_POST[pydh]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/template.html\", \"username\":\"rbruno\", \"nsize\":\"10\" }","name":"pydh"}'
BENCHMARK_POST[pyco]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/snap.png\" }","name":"pyco"}'
BENCHMARK_POST[pytn]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/snap.png\" }","name":"pytn"}'
BENCHMARK_POST[pyup]='{"arguments":"{ \"url\":\"http://127.0.0.1:8000/snap.png\" }","name":"pyup"}'

function start_hydra {
    export app_dir=$(pwd)/apps
    mkdir -p $app_dir
    bash $(DIR)/../../../graalvisor/graalvisor graalvisor.pid &>> graalvisor.log &

    # Wait for hydra to launch.
    timeout 1s bash -c "while ! nc -z $HYDRA_HOST $HYDRA_PORT; do sleep 0.1; done"

    # TODO - after 1 second, if the port is not open, give up with an error.
}

function stop_hydra {
    # Note: wait until pid file is filled.
    timeout 1s bash -c "while [ ! -s hydra.pid ]; do sleep 0.1; done"

    # Kill hydra.
    if [ -f graalvisor.pid ]; then
        echo "killing graalvisor running with pid $(cat graalvisor.pid)"
        kill $(cat graalvisor.pid)
        rm graalvisor.pid
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
    echo ${BENCHMARK_POST["$bench"]} > $APP_POST

    ab -l -p $APP_POST -T application/json -c $conc -n $reqs http://$HYDRA_ADDRESS/${BENCHMARK_RUN_ENDPOINT["$bench"]} &> $bench-ab.log
    rm $APP_POST
    echo "Ran function $bench"
    cat $bench-ab.log
}
