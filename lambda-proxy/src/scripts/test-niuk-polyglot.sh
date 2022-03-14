#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

function setup_polyglot_niuk {
	mkdir $tmpdir &> /dev/null
	sudo ls $tmpdir &> /dev/null
	cp $ARGO_RESOURCES/truffle-build/polyglot-proxy.img $tmpdir
	cp $ARGO_RESOURCES/truffle-build/polyglot-proxy_unikernel.sh $tmpdir/app_unikernel.sh
}

function start_polyglot_niuk {
	proxy_args="lambda_timestamp=$(date +%s%N | cut -b1-13) lambda_port=8080 LD_LIBRARY_PATH=/lib:/lib64:/apps:/usr/local/lib"
	start_niuk
}

setup_polyglot_niuk
start_polyglot_niuk &> $tmpdir/lambda.log &
sleep 5
run_test_polyglot_java
#run_test_polyglot_javascript
sleep 1
run_workload
stop_niuk &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"
