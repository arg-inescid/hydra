#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../cr-single-function/shared.sh

BENCH_ARRAY=(jvcy)

BENCHMARK_REGISTER_QUERY[jvhw]="name=jvhw&language=java&entrypoint=com.hello_world.HelloWorld&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-hello-world.so"
BENCHMARK_REGISTER_QUERY[jvfh]="name=jvfh&language=java&entrypoint=com.filehashing.FileHashing&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-file-hashing.so"
BENCHMARK_REGISTER_QUERY[jvcy]="name=jvcy&language=java&entrypoint=com.classify.Classify&sandbox=process&url=http://127.0.0.1:8000/apps/gv-jv-classify.zip"
BENCHMARK_REGISTER_QUERY[jvhr]="name=jvhr&language=java&entrypoint=com.httprequest.HttpRequest&sandbox=isolate&url=http://127.0.0.1:8000/apps/gv-jv-httprequest.so"
BENCHMARK_REGISTER_QUERY[jvvp]="name=jvvp&language=java&entrypoint=com.videoprocessing.VideoProcessing&sandbox=process&url=http://127.0.0.1:8000/apps/gv-jv-video-processing.so"

BENCHMARK_REGISTER_QUERY[jshw]="name=jshw&language=java&entrypoint=com.helloworld.HelloWorld&svmid=1&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-hello-world.so"
BENCHMARK_REGISTER_QUERY[jsdh]="name=jsdh&language=java&entrypoint=com.dynamichtml.DynamicHTML&svmid=2&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-js-dynamic-html.so"
BENCHMARK_REGISTER_QUERY[jsup]="name=jsup&language=java&entrypoint=com.uploader.Uploader&svmid=4&sandbox=context&url=http://127.0.0.1:8000/apps/gv-js-uploader.so"
BENCHMARK_REGISTER_QUERY[jstn]="name=jstn&language=java&entrypoint=com.thumbnail.Thumbnail&sandbox=context&url=http://127.0.0.1:8000/apps/gv-js-thumbnail.zip"

BENCHMARK_REGISTER_QUERY[pyhw]="name=pyhw&language=java&entrypoint=com.helloworld.HelloWorld&svmid=5&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-hello-world.so"
BENCHMARK_REGISTER_QUERY[pymst]="name=pymst&language=java&entrypoint=com.mst.MST&svmid=6&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-mst.so"
BENCHMARK_REGISTER_QUERY[pybfs]="name=pybfs&language=java&entrypoint=com.bfs.BFS&svmid=7&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-bfs.so"
BENCHMARK_REGISTER_QUERY[pypr]="name=pypr&language=java&entrypoint=com.pr.PageRank&svmid=8&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-pagerank.so"
BENCHMARK_REGISTER_QUERY[pydna]="name=pydna&language=java&entrypoint=com.dna.DNA&svmid=9&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-dna.so"
BENCHMARK_REGISTER_QUERY[pydh]="name=pydh&language=java&entrypoint=com.dynamichtml.DynamicHTML&svmid=10&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-dynamic-html.so"
BENCHMARK_REGISTER_QUERY[pyco]="name=pyco&language=java&entrypoint=com.compression.Compression&svmid=11&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-compression.so"
BENCHMARK_REGISTER_QUERY[pytn]="name=pytn&language=java&entrypoint=com.thumbnail.Thumbnail&svmid=12&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-thumbnail.so"
BENCHMARK_REGISTER_QUERY[pyup]="name=pyup&language=java&entrypoint=com.uploader.Uploader&svmid=13&sandbox=snapshot&url=http://127.0.0.1:8000/apps/gv-py-uploader.so"
BENCHMARK_REGISTER_QUERY[pyvp]="name=pyvp&language=java&entrypoint=com.videoprocessing.VideoProcessing&sandbox=context&url=http://127.0.0.1:8000/apps/gv-py-video-processing.so"


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
