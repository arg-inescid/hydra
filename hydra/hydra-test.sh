#!/bin/bash

# Note: ensure that you have installed the apps by running $ARGO_HOME/benchmarks/scripts/install_benchmarks.sh.

HY_HOST=localhost
HY_PORT=8080
DATA_HOST=localhost
DATA_PORT=8000

# Delete all hy-jv-hello-world artifacts before uploading.
rm -r /tmp/apps/hy-jv-hello-world.*

# Register the function.
curl -s -X POST $HY_HOST:$HY_PORT/register?name=hw\&language=java\&entryPoint=com.hello_world.HelloWorld\&sandbox=snapshot\&url=http://$DATA_HOST:$DATA_PORT/apps/hy-jv-hello-world.so
echo ""
# Invoke the function for the first time (will checkpoint).
curl --connect-timeout 5 -X POST $HY_HOST:$HY_PORT/warmup?concurrency=1\&requests=1 -H 'Content-Type: application/json' -d '{"arguments":"{ }","name":"hw"}'
echo ""
# Invoke the function for the second time (will re-use the sandbox).
curl --connect-timeout 5 -X POST $HY_HOST:$HY_PORT/warmup?concurrency=1\&requests=1 -H 'Content-Type: application/json' -d '{"arguments":"{ }","name":"hw"}'
echo ""
# Invoke the function for the third time using a different target.
curl --connect-timeout 5 -X POST $HY_HOST:$HY_PORT/test -H 'Content-Type: application/json' -d '{"arguments":"{ }","name":"hw"}'
echo ""
