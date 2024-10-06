#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

USERNAME=user

BENCH_ARRAY=(jshw jsup jsdh pyhw pyup pyco)
# BENCH_ARRAY=(jshw jsup jsdh)

declare -A BENCHMARK_BINARIES
BENCHMARK_BINARIES[jshw]="$ARGO_HOME/../benchmarks/src/javascript/gv-hello-world/build/libhelloworld.so"
BENCHMARK_BINARIES[jsup]="$ARGO_HOME/../benchmarks/src/javascript/gv-uploader/build/libuploader.so"
BENCHMARK_BINARIES[jsdh]="$ARGO_HOME/../benchmarks/src/javascript/gv-dynamic-html/build/libdynamichtml.so"
BENCHMARK_BINARIES[pyhw]="$ARGO_HOME/../benchmarks/src/python/gv-hello-world/build/libhelloworld.so"
BENCHMARK_BINARIES[pyup]="$ARGO_HOME/../benchmarks/src/python/gv-uploader/build/libuploader.so"
BENCHMARK_BINARIES[pyco]="$ARGO_HOME/../benchmarks/src/python/gv-compression/build/libcompression.so"

declare -A BENCHMARK_ENTRYPOINTS
BENCHMARK_ENTRYPOINTS[jshw]="com.helloworld.HelloWorld"
BENCHMARK_ENTRYPOINTS[jsup]="com.uploader.Uploader"
BENCHMARK_ENTRYPOINTS[jsdh]="com.dynamichtml.DynamicHTML"
BENCHMARK_ENTRYPOINTS[pyhw]="com.helloworld.HelloWorld"
BENCHMARK_ENTRYPOINTS[pyup]="com.uploader.Uploader"
BENCHMARK_ENTRYPOINTS[pyco]="com.compression.Compression"

declare -A BENCHMARK_PAYLOADS
BENCHMARK_PAYLOADS[jshw]='{}'
BENCHMARK_PAYLOADS[jsup]='{"url":"http://172.18.0.1:8000/snap.png"}'
BENCHMARK_PAYLOADS[jsdh]='{"url":"http://172.18.0.1:8000/template.html","username":"rbruno","nsize":"10"}'
BENCHMARK_PAYLOADS[pyhw]='{}'
BENCHMARK_PAYLOADS[pyup]='{"url":"http://172.18.0.1:8000/snap.png"}'
BENCHMARK_PAYLOADS[pyco]='{"url":"http://172.18.0.1:8000/video.mp4"}'

declare -A BENCHMARK_SVMIDS
BENCHMARK_SVMIDS[jshw]="1"
BENCHMARK_SVMIDS[jsup]="2"
BENCHMARK_SVMIDS[jsdh]="3"
BENCHMARK_SVMIDS[pyhw]="4"
BENCHMARK_SVMIDS[pyup]="5"
BENCHMARK_SVMIDS[pyco]="6"


# Upload configuration (make sure configuration has the lambda pool configured properly).
curl -s -X POST localhost:30009/configure_manager -H 'Content-Type: application/json' --data-binary @"$ARGO_HOME/run/configs/manager/default-lambda-manager.json"

# Register, invoke, and save a snapshot of each function.
for bench in "${BENCH_ARRAY[@]}"; do
    # Define bench-specific variables.
    bench_dir=$(dirname ${BENCHMARK_BINARIES["$bench"]})
    bench_filename=$(basename ${BENCHMARK_BINARIES["$bench"]})
    bench_filename="${bench_filename%.*}"
    full_bench_name="$USERNAME"_"$bench"

    # rm -f $bench_dir/$bench_filename.memsnap
    # rm -f $bench_dir/$bench_filename.metasnap

    # Register.
    curl -s -X POST localhost:30009/upload_function?username=$USERNAME\&function_name=$bench\&function_language=java\&function_entry_point=${BENCHMARK_ENTRYPOINTS["$bench"]}\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=context-snapshot\&svm_id=${BENCHMARK_SVMIDS["$bench"]} -H 'Content-Type: application/octet-stream' --data-binary ${BENCHMARK_BINARIES["$bench"]}
    # Invoke.
    curl -s -X POST localhost:30009/$USERNAME/$bench -H 'Content-Type: application/json' --data ${BENCHMARK_PAYLOADS["$bench"]}

    # Save snapshot files.
    # sudo chown -R $(id -u -n):$(id -g -n) $ARGO_HOME/lambda-manager/codebase/$full_bench_name/*snap
    # cp $ARGO_HOME/lambda-manager/codebase/$full_bench_name/*snap $bench_dir
    # mv $bench_dir/$full_bench_name.memsnap $bench_dir/$bench_filename.memsnap
    # mv $bench_dir/$full_bench_name.metasnap $bench_dir/$bench_filename.metasnap
done
