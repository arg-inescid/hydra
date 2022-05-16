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
APP_POST=$(DIR)/hello-world.post

#APP_JAR=$ARGO_HOME/../benchmarks/language/java/sleep/build/libs/sleep-1.0.jar
#APP_MAIN=com.sleep.Sleep
#APP_CONFIG=$(DIR)/config-sleep
#APP_POST=$(DIR)/sleep.post

#APP_POST=$(DIR)/tf.post

function pretime {
	ts=$(date +%s%N)
}

function postime {
	tt=$((($(date +%s%N) - $ts)/1000))
	printf "\nTime taken: $tt us"
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
		$proxy_args # TODO - check the pid, make sure it is the correct one.
}

function start_svm {
	cd $tmpdir
	./app $proxy_args &
	pid=$!
	echo $! > $tmpdir/lambda.pid
	wait
}

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

function run_test_polyglot_java {
	curl -X POST $ip:8080/register?name=com.hello_world.HelloWorld\&entryPoint=com.hello_world.HelloWorld\&language=java -H 'Content-Type: application/json' \
		--data-binary "@/home/rbruno/git/graalvm-argo/benchmarks/language/java/hello-world/build/libhelloworld.so"
	for i in {1..10}
	do
		curl --no-progress-meter -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"com.hello_world.HelloWorld","arguments":""}'
	done

	curl -X POST $ip:8080/register?name=com.hello_world.HelloWorld\&entryPoint=com.hello_world.HelloWorld\&language=java -H 'Content-Type: application/json' \
		--data-binary "@/home/rbruno/git/graalvm-argo/benchmarks/language/java/hello-world/build/libhelloworld.so"
	for i in {1..1000}
	do
		pretime
		curl --no-progress-meter -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"com.hello_world.HelloWorld","arguments":""}'
		postime
	done
}

function run_test_polyglot_javascript {
	curl --no-progress-meter -X POST $ip:8080/register?name=jsf1\&entryPoint=x\&language=javascript -H 'Content-Type: application/json' \
		-d 'function x(args) { return { "result": "Hello world from js jsf1!" }; };'
	for i in {1..10}
	do
		time curl -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"jsf1","arguments":""}'
	done

	curl --no-progress-meter -X POST $ip:8080/register?name=jsf2\&entryPoint=x\&language=javascript -H 'Content-Type: application/json' \
		-d 'function x(args) { return { "result": "Hello world from jsf2!" }; };'
	for i in {1..1000}
	do
		pretime
		curl --no-progress-meter -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"jsf2","arguments":""}'
		#time curl -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"jsf2","async":"true","arguments":""}'
		postime
	done

}

function run_test_polyglot_python {
	curl --no-progress-meter -X POST $ip:8080/register?name=pyf1\&entryPoint=x\&language=python -H 'Content-Type: application/json' \
		-d 'def x(args): return { "result": "Hello world from pyf1!" }'
	for i in {1..10}
	do
		curl --no-progress-meter -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"pyf1","arguments":""}'
	done
	curl --no-progress-meter -X POST $ip:8080/register?name=pyf2\&entryPoint=x\&language=python -H 'Content-Type: application/json' \
		-d 'def x(args): return { "result": "Hello world from pyf2!" }'
	for i in {1..1000}
	do
		pretime
		curl --no-progress-meter -X POST $ip:8080 -H 'Content-Type: application/json' -d '{"name":"pyf2","arguments":""}'
		postime
	done

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
