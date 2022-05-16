#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

ip=127.0.0.1

function setup_polyglot_svm {
	mkdir $tmpdir &> /dev/null
	sudo ls $tmpdir &> /dev/null
	cp $ARGO_RESOURCES/truffle-build/polyglot-proxy $tmpdir/app
}

function start_polyglot_svm {
	proxy_args="$(date +%s%N | cut -b1-13) $APP_MAIN 8080"
	start_svm
}

# Build ../../build.sh --polyglot-baremetal
setup_polyglot_svm
start_polyglot_svm &> $tmpdir/lambda.log &
sleep 5
#run_test_polyglot_java &> $tmpdir/lambda-java.dat
#run_test_polyglot_javascript &> $tmpdir/lambda-javascript.dat
run_test_polyglot_python &> $tmpdir/lambda-python.dat
sleep 1
#run_workload
stop_baremetal &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"
