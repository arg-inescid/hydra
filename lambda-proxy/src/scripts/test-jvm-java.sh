#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

ip=127.0.0.1

function setup_java_baremetal {
	mkdir $tmpdir &> /dev/null
	sudo chown -R $USER:$USER $tmpdir
}

function start_java_baremetal {
	proxy_args="$(date +%s%N | cut -b1-13) $APP_MAIN 8080"
	proxy_main=org.graalvm.argo.lambda_proxy.BaremetalJavaProxy
	start_jvm
}

setup_java_baremetal
start_java_baremetal &> $tmpdir/lambda.log &
sleep 5
run_test_java
run_workload
stop_baremetal &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"
