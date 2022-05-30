#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

function start_python {
	docker run --rm --network host --name action-python openwhisk/action-python-v3.9
}

# Note: inspired by https://github.com/apache/openwhisk-runtime-python/blob/master/tutorials/local_build.md
function run_test_python {
	curl --no-progress-meter localhost:8080/init  -H "Content-Type: application/json" -d @test-python/init.json
	for i in {1..1000}
	do
		pretime
		curl --no-progress-meter http://localhost:8080/run -H "Content-Type: application/json" -d @test-python/run.json
		postime
		done
}

function stop_python {
	docker stop action-python
}

start_python &> $tmpdir/python.log &

sleep 1

run_test_python > $tmpdir/python.dat
stop_python
