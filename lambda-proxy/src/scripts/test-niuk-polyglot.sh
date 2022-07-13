#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh

# Build ../../build.sh --polyglot
setup_polyglot_niuk
start_polyglot_niuk &> $tmpdir/lambda.log &
sleep 5

polyglot_java_hello_world
run_test_polyglot_java

#polyglot_javascript_hello_world
#run_test_polyglot_javascript

#polyglot_python_hello_world
#run_test_polyglot_python

stop_niuk &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"
