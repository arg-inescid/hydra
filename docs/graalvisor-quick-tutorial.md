## Introduction

The objective of this quick tutorial is to build and run GraalVisor, build a simple guest application, upload it to the GraalVisor runtime and invoke it.

Prerequisites for this tutorial:

+ JDK11+ and the `JAVA_HOME` environment variable pointing to it (`export JAVA_HOME=/path/to/jdk/root/directory`).
+ `Argo` repository and the `ARGO_HOME` environment variable pointing to it (`export ARGO_HOME=/path/to/argo`).
+ `Benchmarks` repository that should be put as a sibling to the `Argo` repository.

## How to build

1. Build GraalVisor Library
   * Execute the `/path/to/argo/graavisor-lib/build.sh` script.
2. Build GraalVisor
   * Execute the `/path/to/argo/graavisor/build.sh` script. When asked about JavaScript/Python support and NIUk build, input `no` - we don't need these features for this tutorial.
3. Build guest application
   * Execute the `/path/to/benchmarks/src/java/gv-hello-world/build_script.sh` script.

## How to run and invoke

You will need two terminals (**T1** and **T2**):

1. (T1) `export lambda_port=8080`.
2. (T1) `export lambda_timestamp="$(date +%s%N | cut -b1-13)"`.
3. (T1) Run the `/path/to/argo/graavisor/build/native-image/polyglot-proxy` binary. Now you have GraalVisor running and listening at port 8080.
4. (T2) `curl -s -X POST localhost:8080/register?name=helloworld\&entryPoint=com.hello_world.HelloWorld\&language=java -H 'Content-Type: application/json' --data-binary @"/path/to/benchmarks/src/java/gv-hello-world/build/libhelloworld.so"`. Now you have the HelloWorld function uploaded to GraalVisor.
5. (T2) `curl -s -X POST localhost:8080 -H 'Content-Type: application/json' --data-binary '{"name":"helloworld","async":"false","arguments":""}'` - invoke the function.
