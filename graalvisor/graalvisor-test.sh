#!/bin/bash

# Note: ensure that you have installed the apps by running $ARGO_HOME/benchmarks/scripts/install_benchmarks.sh.

GV_HOST=localhost
GV_PORT=8080
DATA_HOST=localhost
DATA_PORT=8000

# Delete all gv-jv-hello-world artifacts before uploading.
rm -r /tmp/apps/gv-jv-hello-world.*

curl -s -X POST $GV_HOST:$GV_PORT/register?name=hw\&language=java\&entryPoint=com.hello_world.HelloWorld\&sandbox=snapshot\&url=http://$DATA_HOST:$DATA_PORT/apps/gv-jv-hello-world.so
curl --connect-timeout 5 -X POST $GV_HOST:$GV_PORT/warmup?concurrency=1\&requests=1 -H 'Content-Type: application/json' -d '{"arguments":"{ }","name":"hw"}'
