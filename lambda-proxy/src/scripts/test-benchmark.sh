#!/bin/bash

# Getting the local default ip used to connect to the internet.
IP=$(ip route get 8.8.8.8 | grep -oP  'src \K\S+')

function gv_java_hw {
	APP_LANG=java
	APP_NAME=gv-hello-world
	APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libhelloworld.so
	APP_MAIN=com.hello_world.HelloWorld
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"hw","async":"false","cached":"true","arguments":""}' > $APP_POST
}

function cr_java_hw {
	IMG=docker.io/openwhisk/java8action:latest
	APP_LANG=java
	APP_NAME=cr-hello-world
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
}

function gv_javascript_hw {
	APP_LANG=javascript
	APP_NAME=gv-hello-world
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/hello-world.js
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function cr_javascript_hw {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	APP_LANG=javascript
	APP_NAME=cr-hello-world
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run2.json
}

function gv_python_hw {
	APP_LANG=python
	APP_NAME=gv-hello-world
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/hello-world.py
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function cr_python_hw {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	APP_LANG=python
	APP_NAME=cr-hello-world
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run2.json
}

function gv_python_thumbnail {
	APP_LANG=python
	APP_NAME=gv-thumbnail
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.py
	curl -s -X POST $ip:8080/register?name=thumbnail\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"thumbnail","async":"false","arguments":"http://'$IP':8000/snap.png"}' > $APP_POST
}

function cr_python_thumbnail {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	APP_LANG=python
	APP_NAME=cr-thumbnail
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/snap.png" } }' > $RUN_POST
}
function gv_javascript_thumbnail {
	APP_LANG=javascript
	APP_NAME=gv-thumbnail
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.js
	curl -s -X POST $ip:8080/register?name=thumbnail\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"thumbnail","async":"false","arguments":"http://'$IP':8000/snap.png"}' > $APP_POST
}

function cr_javascript_thumbnail {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	APP_LANG=javascript
	APP_NAME=cr-thumbnail
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/snap.png" } }' > $RUN_POST
}

function gv_java_sleep {
	APP_LANG=java
	APP_NAME=gv-sleep
	APP_MAIN=com.sleep.Sleep
	APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libsleep.so
	curl -s -X POST $ip:8080/register?name=sleep\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"sleep","async":"false","arguments":"{\"memory\":\"128\",\"sleep\":\"1000\"}"}' > $APP_POST
}

function cr_java_sleep {
	IMG=docker.io/openwhisk/java8action:latest
	APP_LANG=java
	APP_NAME=cr-sleep
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
}

function gv_python_sleep {
	APP_LANG=python
	APP_NAME=gv-sleep
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/sleep.py
	curl -s -X POST $ip:8080/register?name=sleep\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"sleep","async":"false","arguments":"1"}' > $APP_POST
}

function cr_python_sleep {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	APP_LANG=python
	APP_NAME=cr-sleep
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
}

function gv_javascript_sleep {
	APP_LANG=javascript
	APP_NAME=gv-sleep
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/sleep.js
	curl -s -X POST $ip:8080/register?name=sleep\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"sleep","async":"false","arguments":"1000"}' > $APP_POST
}

function cr_javascript_sleep {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	APP_LANG=javascript
	APP_NAME=cr-sleep
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
}

function gv_java_filehashing {
	APP_LANG=java
	APP_NAME=gv-file-hashing
	APP_MAIN=com.filehashing.FileHashing
	APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libfilehashing.so
	curl -s -X POST $ip:8080/register?name=filehashing\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"filehashing","async":"false","arguments":"{\"url\":\"http://'$IP':8000/snap.png\"}"}' > $APP_POST
}

function cr_java_filehashing {
	IMG=docker.io/openwhisk/java8action:latest
	APP_LANG=java
	APP_NAME=cr-file-hashing
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/snap.png" } }' > $RUN_POST
}

function gv_java_httprequest {
	APP_LANG=java
	APP_NAME=gv-httprequest
	APP_MAIN=com.httprequest.HttpRequest
	APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libhttprequest.so
	curl -s -X POST $ip:8080/register?name=httprequest\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"httprequest","async":"false","arguments":"{\"url\":\"http://'$IP':8000/snap.png\"}"}' > $APP_POST
}

function cr_java_httprequest {
	IMG=docker.io/openwhisk/java8action:latest
	APP_LANG=java
	APP_NAME=cr-httprequest
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/snap.png" } }' > $RUN_POST
}

function gv_java_videoprocessing {
	APP_LANG=java
	APP_NAME=gv-video-processing
	APP_MAIN=com.videoprocessing.VideoProcessing
	APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libvideoprocessing.so
	curl -s -X POST $ip:8080/register?name=videoprocessing\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"videoprocessing","async":"false","arguments":"{\"video\":\"http://'$IP':8000/video.mp4\",\"ffmpeg\":\"http://'$IP':8000/ffmpeg\"}"}' > $APP_POST
}

function cr_java_videoprocessing {
	IMG=docker.io/openwhisk/java8action:latest
	APP_LANG=java
	APP_NAME=cr-video-processing
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "ffmpeg_url": "http://'$IP':8000/ffmpeg", "video_url": "http://'$IP':8000/video.mp4" } }' > $RUN_POST
}

function gv_python_videoprocessing {
	APP_LANG=python
	APP_NAME=gv-video-processing
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.py
	curl -s -X POST $ip:8080/register?name=videoprocessing\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"videoprocessing","async":"false","arguments":"http://'$IP':8000/ffmpeg;http://'$IP':8000/video.mp4"}' > $APP_POST
}

function cr_python_videoprocessing {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	APP_LANG=python
	APP_NAME=cr-video-processing
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "ffmpeg_url": "http://'$IP':8000/ffmpeg", "video_url": "http://'$IP':8000/video.mp4" } }' > $RUN_POST
}

function gv_java_classify {
	APP_LANG=java
	APP_NAME=gv-classify
	APP_MAIN=com.classify.Classify
	APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libclassify.so
	curl -s -X POST $ip:8080/register?name=classify\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"classify","async":"false","arguments":"{\"model_url\":\"http://'$IP':8000/tensorflow_inception_graph.pb\",\"labels_url\":\"http://'$IP':8000/imagenet_comp_graph_label_strings.txt\",\"image_url\":\"http://'$IP':8000/eagle.jpg\"}"}' > $APP_POST
}

function cr_java_classify {
	IMG=docker.io/openwhisk/java8action:latest
	APP_LANG=java
	APP_NAME=cr-classify
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "model_url": "http://'$IP':8000/tensorflow_inception_graph.pb", "labels_url": "http://'$IP':8000/imagenet_comp_graph_label_strings.txt", "image_url": "http://'$IP':8000/eagle.jpg" } }' > $RUN_POST
}

function gv_python_compression {
	APP_LANG=python
	APP_NAME=gv-compression
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.py
	curl -s -X POST $ip:8080/register?name=compression\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"compression","async":"false","arguments":"http://'$IP':8000/video.mp4"}' > $APP_POST
}

function cr_python_compression {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	APP_LANG=python
	APP_NAME=cr-compression
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/video.mp4" } }' > $RUN_POST
}

function gv_javascript_dynamichtml {
	APP_LANG=javascript
	APP_NAME=gv-dynamic-html
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.js
	curl -s -X POST $ip:8080/register?name=dynamichtml\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"dynamichtml","async":"false","arguments":"http://'$IP':8000/template.html;rbruno;10"}' > $APP_POST
}

function cr_javascript_dynamichtml {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	APP_LANG=javascript
	APP_NAME=cr-dynamic-html
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/template.html", "username": "rbruno", "nsize": "10" } }' > $RUN_POST
}

function gv_python_dynamichtml {
	APP_LANG=python
	APP_NAME=gv-dynamic-html
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.py
	curl -s -X POST $ip:8080/register?name=dynamichtml\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"dynamichtml","async":"false","arguments":"http://'$IP':8000/template.html;rbruno;10"}' > $APP_POST
}

function cr_python_dynamichtml {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	APP_LANG=python
	APP_NAME=cr-dynamic-html
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/template.html", "username": "rbruno", "nsize": "10" } }' > $RUN_POST
}

function gv_python_uploader {
	APP_LANG=python
	APP_NAME=gv-uploader
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.py
	curl -s -X POST $ip:8080/register?name=uploader\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"uploader","async":"false","arguments":"http://'$IP':8000/snap.png"}' > $APP_POST
}

function cr_python_uploader {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	APP_LANG=python
	APP_NAME=cr-uploader
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/snap.png" } }' > $RUN_POST
}


function gv_javascript_uploader {
	APP_LANG=javascript
	APP_NAME=gv-uploader
	APP_MAIN=main
	APP_SCRIPT=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/main.js
	curl -s -X POST $ip:8080/register?name=uploader\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SCRIPT
	echo '{"name":"uploader","async":"false","arguments":"http://'$IP':8000/snap.png"}' > $APP_POST
}

function cr_javascript_uploader {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	APP_LANG=javascript
	APP_NAME=cr-uploader
	INIT_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/init.json
	RUN_POST=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/run.json
	echo '{ "value": { "url": "http://'$IP':8000/snap.png" } }' > $RUN_POST
}

# Old, Jar-based benchmarks.
function java_hw {
	APP_LANG=java
	APP_NAME=gv-hello-world
	APP_JAR=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libs/hello-world-1.0.jar
	APP_MAIN=com.hello_world.HelloWorld
	APP_CONFIG=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/ni-agent-config
	echo '{"name":"hw","async":"true","arguments":""}' > $APP_POST
}

function java_sleep {
	APP_LANG=java
	APP_NAME=sleep
	APP_JAR=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libs/sleep-1.0.jar
	APP_MAIN=com.sleep.Sleep
	APP_CONFIG=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/ni-agent-config
	echo '{"name":"com.sleep.Sleep","async":"true","arguments":"{\"memory\":\"128\",\"sleep\":\"1000\"}"}' > $APP_POST
}

