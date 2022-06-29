#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

ARGO_HOME=$(DIR)/../../../
ARGO_RESOURCES=$ARGO_HOME/resources
source $ARGO_HOME/lambda-manager/src/scripts/environment.sh
VMM=`grep target $VIRTUALIZE_PATH | awk -F\" '{print $4}'`

tmpdir=/tmp/test-proxy
mkdir $tmpdir &> /dev/null

function java_hello_world {
	APP_JAR=$ARGO_HOME/../benchmarks/language/java/hello-world/build/libs/hello-world-1.0.jar
	APP_MAIN=com.hello_world.HelloWorld
	APP_CONFIG=$(DIR)/config-hello-world
	APP_POST=$(DIR)/hello-world.post
}

function java_sleep {
	APP_JAR=$ARGO_HOME/../benchmarks/language/java/sleep/build/libs/sleep-1.0.jar
	APP_MAIN=com.sleep.Sleep
	APP_CONFIG=$(DIR)/config-sleep
	APP_POST=$(DIR)/sleep-sync.post
	#APP_POST=$(DIR)/sleep-async.post

}

function polyglot_java_hello_world {
	APP_SO=$ARGO_HOME/../benchmarks/language/java/hello-world/build/libhelloworld.so
	APP_LANG=java
	APP_MAIN=com.hello_world.HelloWorld
}

function polyglot_javascript_hello_world {
	APP_SCRIPT=$(DIR)/hello-world.js
	APP_LANG=javascript
	APP_MAIN=main
}

function polyglot_python_hello_world {
	APP_SCRIPT=$(DIR)/hello-world.py
	APP_LANG=python
	APP_MAIN=main
}

function pretime {
	ts=$(date +%s%N)
}

function postime {
	tt=$((($(date +%s%N) - $ts)/1000))
	printf "\nTime taken: $tt us"
}

function log_rss {
	PID=$1
	OFILE=$2
	rm $OFILE
        while kill -0 $PID &> /dev/null; do
                ps -q $PID -o rss= >> $OFILE
                sleep .5
        done
}

function stop_niuk {
	ppid=`sudo cat $tmpdir/lambda.pid`
	for child in $(ps -o pid --no-headers --ppid $ppid); do
		sudo kill $child 
	done
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/remove_taps.sh testtap
	sudo rm -f $tmpdir/*.socket
}

function stop_baremetal {
	pid=`sudo cat $tmpdir/lambda.pid`
	sudo kill $pid
}


function start_niuk {
	cd $tmpdir
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/create_taps.sh testtap $ip
	sudo bash app_unikernel.sh \
		--memory 16384 \
		--ip $ip \
		--gateway $gateway \
		--mask $mask \
		--tap testtap \
		--console \
		--no-karg-patch \
		$proxy_args 
	# TODO - check the pid, lauch rss log.
}

function start_svm {
	cd $tmpdir
	./app $proxy_args &
	pid=$!
	echo $! > $tmpdir/lambda.pid
	log_rss $pid $tmpdir/lambda.rss &
	wait
}

function start_jvm {
	$JAVA_HOME/bin/java -cp $PROXY_JAR:$APP_JAR $proxy_main $proxy_args &
	pid=$!
	echo $! > $tmpdir/lambda.pid
	log_rss $pid $tmpdir/lambda.rss &
	wait
}

function setup_polyglot_svm {
	mkdir $tmpdir &> /dev/null
	sudo ls $tmpdir &> /dev/null
	cp $ARGO_RESOURCES/truffle-build/polyglot-proxy $tmpdir/app
}

function start_polyglot_svm {
	proxy_args="$(date +%s%N | cut -b1-13) 8080"
	start_svm
}

function run_test_java {
	for i in {1..10}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d @$APP_POST
		postime
	done
	for i in {1..1000}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d @$APP_POST
		postime
	done

}

function run_test_polyglot_java {
	curl -s -X POST $ip:8080/register?name=jvhw1\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	for i in {1..10}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"jvhw1","arguments":""}'
		postime
	done

	curl -s -X POST $ip:8080/register?name=jvhw2\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	for i in {1..1000}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"jvhw2","arguments":""}'
		postime
	done
}

function run_test_polyglot_javascript {
	curl -s -X POST $ip:8080/register?name=jshw1\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' -d @$APP_SCRIPT
	for i in {1..10}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"jshw1","arguments":""}'
		postime
	done

	curl -s -X POST $ip:8080/register?name=jshw2\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' -d @$APP_SCRIPT
	for i in {1..1000}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"jshw2","arguments":""}'
		postime
	done
}

function run_test_polyglot_python {
	curl -s -X POST $ip:8080/register?name=pyhw1\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' -d @$APP_SCRIPT
	for i in {1..10}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"pyhw1","arguments":""}'
		postime
	done
	curl -s -X POST $ip:8080/register?name=pyhw2\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' -d @$APP_SCRIPT
	for i in {1..1000}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"pyhw2","arguments":""}'
		postime
	done

}
