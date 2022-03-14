#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

ARGO_HOME=$(DIR)/../../../
ARGO_RESOURCES=$ARGO_HOME/resources
source $ARGO_HOME/lambda-manager/src/scripts/environment.sh
VMM=`grep target $VIRTUALIZE_PATH | awk -F\" '{print $4}'`

tmpdir=/tmp/test-proxy

#APP_JAR=$ARGO_HOME/../benchmarks/language/java/hello-world/build/libs/hello-world-1.0.jar
#APP_MAIN=com.hello_world.HelloWorld
#APP_CONFIG=$(DIR)/config-hello-world
#APP_POST=$(DIR)/hello-world.post

APP_JAR=$ARGO_HOME/../benchmarks/language/java/sleep/build/libs/sleep-1.0.jar
APP_MAIN=com.sleep.Sleep
APP_CONFIG=$(DIR)/config-sleep
APP_POST=$(DIR)/sleep.post

#APP_POST=$(DIR)/tf.post

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
		$proxy_args # TODO - check the pid, make sure it is the correct one.
}

function start_svm {
	cd $tmpdir
	./app $proxy_args &
	pid=$!
	echo $! > $tmpdir/lambda.pid
	wait
}

# TODO - change name to start_jvm
function start_jvm {
	$JAVA_HOME/bin/java -cp $PROXY_JAR:$APP_JAR $proxy_main $proxy_args &
	pid=$!
	echo $! > $tmpdir/lambda.pid
	wait
}

function run_test_java {
	for i in {1..10}
	do
		time curl -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"com.sleep.Sleep","async":"true","arguments":"{\"memory\":\"128\",\"sleep\":\"1000\"}"}'
	done
	time curl -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"com.sleep.Sleep","async":"false","arguments":"{\"memory\":\"128\",\"sleep\":\"1000\"}"}'
}

function run_test_polyglot {
	curl -X POST $ip:8080/register \
		-H 'Content-Type: application/json' \
		-d '{"name":"tf","entryPoint":"x","language":"javascript","sourceCode":"function x(args) { return { result: \"Hello world from tf1!\" }; }"}'

	time curl -X POST $ip:8080 \
		-H 'Content-Type: application/json' \
		-d '{"name":"tf","async":"true","arguments":""}'

	time curl -X POST $ip:8080 \
		-H 'Content-Type: application/json' \
		-d '{"name":"tf","async":"true","arguments":""}'

	curl -X POST $ip:8080/register \
		-H 'Content-Type: application/json' \
		-d '{"name":"tf2","entryPoint":"x","language":"javascript","sourceCode":"function x(args) { return { result: \"Hello world from tf2!\" }; }"}'

	time curl -X POST $ip:8080 \
		-H 'Content-Type: application/json' \
		-d '{"name":"tf2","arguments":""}'

	time curl -X POST $ip:8080 \
		-H 'Content-Type: application/json' \
		-d '{"name":"tf2","arguments":""}'
}

function run_workload {
	# TODO - start memory measurements
	# TODO - replace ab with wrk
	#ab -p $APP_POST -T application/json -c 32 -n 262144 http://$ip:8080/
	#ab -p $APP_POST -T application/json -c 32 -n 131072 http://$ip:8080/
	#ab -p $APP_POST -T application/json -c 32 -n 65536 http://$ip:8080/
	#ab -p $APP_POST -T application/json -c 32 -n 32768 http://$ip:8080/
	#ab -p $APP_POST -T application/json -c 32 -n 16384 http://$ip:8080/
	#ab -p $APP_POST -T application/json -c 32 -n 8192 http://$ip:8080/
	#ab -p $APP_POST -T application/json -c 32 -n 4096 http://$ip:8080/
	#ab -p $APP_POST -T application/json -c 32 -n 2048 http://$ip:8080/
	ab -p $APP_POST -T application/json -c 32 -n 1024 http://$ip:8080/
}
