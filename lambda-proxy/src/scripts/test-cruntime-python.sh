#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh

CRUNTIME_HOME=$(DIR)/../../../lambda-manager/src/scripts/cruntime/

TAP=pytap
VMID=pyvm5

function start_python {
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/create_taps.sh $TAP $ip
	sudo $CRUNTIME_HOME/start-vm -ip $ip/$smask -gw $gateway -tap $TAP -id $VMID -img docker.io/openwhisk/action-python-v3.9:latest
}

# Note: inspired by https://github.com/apache/openwhisk-runtime-python/blob/master/tutorials/local_build.md
function run_test_python {
	curl --no-progress-meter --max-time 5 -X POST $ip:8080/init -H 'Content-Type: application/json' -d @test-cruntime-python/init.json
	for i in {1..3}
	do
		pretime
		curl --no-progress-meter --max-time 5 -X POST $ip:8080/run -H "Content-Type: application/json" -d @test-cruntime-python/run2.json
		postime
		done
}

function stop_python {
	sudo $CRUNTIME_HOME/stop-vm -id $VMID
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/remove_taps.sh $TAP
}

# TODO - create init.json from source.
start_python
sleep 5
run_test_python #> $tmpdir/python.dat
stop_python
