#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function gv_java_hw {
	APP_SO=$ARGO_BENCHMARKS/language/java/gv-hello-world/build/libhelloworld.so
	APP_LANG=java
	APP_MAIN=com.hello_world.HelloWorld
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function gv_javascript_hw {
	APP_SCRIPT=$ARGO_BENCHMARKS/language/javascript/gv-hello-world/hello-world.js
	APP_LANG=javascript
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function gv_python_hw {
	APP_SCRIPT=$ARGO_BENCHMARKS/language/python/gv-hello-world/hello-world.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function gv_python_thumbnail {
	# TODO - need to launch a webserver with the image available
	# TODO - it will be important to pre-load an environment with the packages already ready to go.
	APP_SCRIPT=$ARGO_BENCHMARKS/language/python/gv-thumbnail/main.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=thumbnail\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"thumbnail","async":"false","arguments":"http://127.0.0.1:8000/snap.png"}' > $APP_POST
}

function gv_javascript_thumbnail {
	APP_SCRIPT=$ARGO_BENCHMARKS/language/javascript/gv-thumbnail/main.js
	APP_LANG=javascript
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=thumbnail\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"thumbnail","async":"false","arguments":"http://127.0.0.1:8000/snap.png"}' > $APP_POST
}

function cr_java_hw {
	IMG=docker.io/openwhisk/java8action:latest
	INIT_POST=$ARGO_BENCHMARKS/language/java/cr-hello-world/init.json
	RUN_POST=$ARGO_BENCHMARKS/language/java/cr-hello-world/run.json
}

function cr_javascript_hw {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	INIT_POST=$ARGO_BENCHMARKS/language/javascript/cr-hello-world/init.json
	RUN_POST=$ARGO_BENCHMARKS/language/javascript/cr-hello-world/run2.json
}

function cr_python_hw {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$ARGO_BENCHMARKS/language/python/cr-hello-world/init.json
	RUN_POST=$ARGO_BENCHMARKS/language/python/cr-hello-world/run2.json
}

function java_hw {
	APP_JAR=$ARGO_BENCHMARKS/language/java/gv-hello-world/build/libs/hello-world-1.0.jar
	APP_MAIN=com.hello_world.HelloWorld
	APP_CONFIG=$ARGO_BENCHMARKS/language/java/gv-hello-world/ni-agent-config
	echo '{"name":"hw","async":"true","arguments":""}' > $APP_POST
}

function java_sleep {
	APP_JAR=$ARGO_BENCHMARKS/language/java/sleep/build/libs/sleep-1.0.jar
	APP_MAIN=com.sleep.Sleep
	APP_CONFIG=$ARGO_BENCHMARKS/language/java/sleep/ni-agent-config
	echo '{"name":"com.sleep.Sleep","async":"true","arguments":"{\"memory\":\"128\",\"sleep\":\"1000\"}"}' > $APP_POST
}

