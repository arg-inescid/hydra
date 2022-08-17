#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function gv_java_hw {
	APP_SO=$BENCHMARKS_HOME/language/java/gv-hello-world/build/libhelloworld.so
	APP_LANG=java
	APP_MAIN=com.hello_world.HelloWorld
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function cr_java_hw {
	IMG=docker.io/openwhisk/java8action:latest
	INIT_POST=$BENCHMARKS_HOME/language/java/cr-hello-world/init.json
	RUN_POST=$BENCHMARKS_HOME/language/java/cr-hello-world/run.json
}

function gv_javascript_hw {
	APP_SCRIPT=$BENCHMARKS_HOME/language/javascript/gv-hello-world/hello-world.js
	APP_LANG=javascript
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function cr_javascript_hw {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	INIT_POST=$BENCHMARKS_HOME/language/javascript/cr-hello-world/init.json
	RUN_POST=$BENCHMARKS_HOME/language/javascript/cr-hello-world/run2.json
}

function gv_python_hw {
	APP_SCRIPT=$BENCHMARKS_HOME/language/python/gv-hello-world/hello-world.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function cr_python_hw {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$BENCHMARKS_HOME/language/python/cr-hello-world/init.json
	RUN_POST=$BENCHMARKS_HOME/language/python/cr-hello-world/run2.json
}

function gv_python_thumbnail {
	# TODO - need to launch a webserver with the image available
	APP_SCRIPT=$BENCHMARKS_HOME/language/python/gv-thumbnail/main.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=thumbnail\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"thumbnail","async":"false","arguments":"http://127.0.0.1:8000/snap.png"}' > $APP_POST
}

function cr_python_thumbnail {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$BENCHMARKS_HOME/language/python/cr-thumbnail/init.json
	RUN_POST=$BENCHMARKS_HOME/language/python/cr-thumbnail/run.json
}
function gv_javascript_thumbnail {
	APP_SCRIPT=$BENCHMARKS_HOME/language/javascript/gv-thumbnail/main.js
	APP_LANG=javascript
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=thumbnail\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"thumbnail","async":"false","arguments":"http://127.0.0.1:8000/snap.png"}' > $APP_POST
}

function cr_javascript_thumbnail {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	INIT_POST=$BENCHMARKS_HOME/language/javascript/cr-thumbnail/init.json
	RUN_POST=$BENCHMARKS_HOME/language/javascript/cr-thumbnail/run.json
}

function gv_java_sleep {
	APP_SO=$BENCHMARKS_HOME/language/java/gv-sleep/build/libsleep.so
	APP_LANG=java
	APP_MAIN=com.sleep.Sleep
	curl -s -X POST $ip:8080/register?name=sleep\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"sleep","async":"false","arguments":"{\"memory\":\"128\",\"sleep\":\"1000\"}"}' > $APP_POST
}

function cr_java_sleep {
	IMG=docker.io/openwhisk/java8action:latest
	INIT_POST=$BENCHMARKS_HOME/language/java/cr-sleep/init.json
	RUN_POST=$BENCHMARKS_HOME/language/java/cr-sleep/run.json
}

function gv_python_sleep {
	APP_SCRIPT=$BENCHMARKS_HOME/language/python/gv-sleep/sleep.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=sleep\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"sleep","async":"false","arguments":"1"}' > $APP_POST
}

function cr_python_sleep {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$BENCHMARKS_HOME/language/python/cr-sleep/init.json
	RUN_POST=$BENCHMARKS_HOME/language/python/cr-sleep/run.json
}

function gv_javascript_sleep {
	APP_SCRIPT=$BENCHMARKS_HOME/language/javascript/gv-sleep/sleep.js
	APP_LANG=javascript
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=sleep\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"sleep","async":"false","arguments":"1000"}' > $APP_POST
}

function cr_javascript_sleep {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	INIT_POST=$BENCHMARKS_HOME/language/javascript/cr-sleep/init.json
	RUN_POST=$BENCHMARKS_HOME/language/javascript/cr-sleep/run.json
}

function gv_java_filehashing {
	APP_SO=$BENCHMARKS_HOME/language/java/gv-file-hashing/build/libfilehashing.so
	APP_LANG=java
	APP_MAIN=com.filehashing.FileHashing
	curl -s -X POST $ip:8080/register?name=filehashing\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"filehashing","async":"false","arguments":"{\"url\":\"http://127.0.0.1:8000/snap.png\"}"}' > $APP_POST
}

function cr_java_filehashing {
	IMG=docker.io/openwhisk/java8action:latest
	INIT_POST=$BENCHMARKS_HOME/language/java/cr-file-hashing/init.json
	RUN_POST=$BENCHMARKS_HOME/language/java/cr-file-hashing/run.json
}

function gv_java_httprequest {
	APP_SO=$BENCHMARKS_HOME/language/java/gv-httprequest/build/libhttprequest.so
	APP_LANG=java
	APP_MAIN=com.httprequest.HttpRequest
	curl -s -X POST $ip:8080/register?name=httprequest\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"httprequest","async":"false","arguments":"{\"url\":\"http://127.0.0.1:8000/snap.png\"}"}' > $APP_POST
}

function cr_java_httprequest {
	IMG=docker.io/openwhisk/java8action:latest
	INIT_POST=$BENCHMARKS_HOME/language/java/cr-httprequest/init.json
	RUN_POST=$BENCHMARKS_HOME/language/java/cr-httprequest/run.json
}

function gv_java_videoprocessing {
	APP_SO=$BENCHMARKS_HOME/language/java/gv-video-processing/build/libvideoprocessing.so
	APP_LANG=java
	APP_MAIN=com.videoprocessing.VideoProcessing
	curl -s -X POST $ip:8080/register?name=videoprocessing\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"videoprocessing","async":"false","arguments":"{\"video\":\"http://127.0.0.1:8000/file_example_MP4_480_1_5MG.mp4\",\"ffmpeg\":\"http://127.0.0.1:8000/ffmpeg\"}"}' > $APP_POST
}

function cr_java_videoprocessing {
	IMG=docker.io/openwhisk/java8action:latest
	INIT_POST=$BENCHMARKS_HOME/language/java/cr-video-processing/init.json
	RUN_POST=$BENCHMARKS_HOME/language/java/cr-video-processing/run.json
}

function gv_python_videoprocessing {
	APP_SCRIPT=$BENCHMARKS_HOME/language/python/gv-video-processing/main.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=videoprocessing\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"videoprocessing","async":"false","arguments":"http://127.0.0.1:8000/ffmpeg;http://127.0.0.1:8000/file_example_MP4_480_1_5MG.mp4"}' > $APP_POST
}

function cr_python_videoprocessing {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$BENCHMARKS_HOME/language/python/cr-video-processing/init.json
	RUN_POST=$BENCHMARKS_HOME/language/python/cr-video-processing/run.json
}

function gv_python_compression {
	APP_SCRIPT=$BENCHMARKS_HOME/language/python/gv-compression/main.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=compression\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"compression","async":"false","arguments":"http://127.0.0.1:8000/file_example_MP4_480_1_5MG.mp4"}' > $APP_POST
}

function cr_python_compression {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$BENCHMARKS_HOME/language/python/cr-compression/init.json
	RUN_POST=$BENCHMARKS_HOME/language/python/cr-compression/run.json
}

function gv_javascript_dynamichtml {
	APP_SCRIPT=$BENCHMARKS_HOME/language/javascript/gv-dynamic-html/main.js
	APP_LANG=javascript
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=dynamichtml\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"dynamichtml","async":"false","arguments":"http://127.0.0.1:8000/template.html;rbruno;1000"}' > $APP_POST
}

function cr_javascript_dynamichtml {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	INIT_POST=$BENCHMARKS_HOME/language/javascript/cr-dynamic-html/init.json
	RUN_POST=$BENCHMARKS_HOME/language/javascript/cr-dynamic-html/run.json
}

function gv_python_dynamichtml {
	APP_SCRIPT=$BENCHMARKS_HOME/language/python/gv-dynamic-html/main.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=dynamichtml\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"dynamichtml","async":"false","arguments":"http://127.0.0.1:8000/template.html;rbruno;1000"}' > $APP_POST
}

function cr_python_dynamichtml {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$BENCHMARKS_HOME/language/python/cr-dynamic-html/init.json
	RUN_POST=$BENCHMARKS_HOME/language/python/cr-dynamic-html/run.json
}

function gv_python_uploader {
	APP_SCRIPT=$BENCHMARKS_HOME/language/python/gv-uploader/main.py
	APP_LANG=python
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=uploader\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"uploader","async":"false","arguments":"http://127.0.0.1:8000/snap.png"}' > $APP_POST
}

function cr_python_uploader {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=$BENCHMARKS_HOME/language/python/cr-uploader/init.json
	RUN_POST=$BENCHMARKS_HOME/language/python/cr-uploader/run.json
}


function gv_javascript_uploader {
	APP_SCRIPT=$BENCHMARKS_HOME/language/javascript/gv-uploader/main.js
	APP_LANG=javascript
	APP_MAIN=main
	curl -s -X POST $ip:8080/register?name=uploader\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"uploader","async":"false","arguments":"http://127.0.0.1:8000/snap.png"}' > $APP_POST
}

function cr_javascript_uploader {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	INIT_POST=$BENCHMARKS_HOME/language/javascript/cr-uploader/init.json
	RUN_POST=$BENCHMARKS_HOME/language/javascript/cr-uploader/run.json
}

# Old, Jar-based benchmarks.
function java_hw {
	APP_JAR=$BENCHMARKS_HOME/language/java/gv-hello-world/build/libs/hello-world-1.0.jar
	APP_MAIN=com.hello_world.HelloWorld
	APP_CONFIG=$BENCHMARKS_HOME/language/java/gv-hello-world/ni-agent-config
	echo '{"name":"hw","async":"true","arguments":""}' > $APP_POST
}

function java_sleep {
	APP_JAR=$BENCHMARKS_HOME/language/java/sleep/build/libs/sleep-1.0.jar
	APP_MAIN=com.sleep.Sleep
	APP_CONFIG=$BENCHMARKS_HOME/language/java/sleep/ni-agent-config
	echo '{"name":"com.sleep.Sleep","async":"true","arguments":"{\"memory\":\"128\",\"sleep\":\"1000\"}"}' > $APP_POST
}

