#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh

CRUNTIME_HOME=$(DIR)/../../../lambda-manager/src/scripts/cruntime/

#IP=192.168.1.99
#MASK=24
#GW=192.168.1.83
IP=10.0.0.165
MASK=16
GW=10.0.0.201

TAP=tap001
VMID=pyvm5

function start_python {
	sudo $CRUNTIME_HOME/start-vm -ip $IP/$MASK -gw $GW -tap $TAP -id $VMID -img docker.io/openwhisk/action-python-v3.9:latest
}

# Note: inspired by https://github.com/apache/openwhisk-runtime-python/blob/master/tutorials/local_build.md
function run_test_python {
	curl --no-progress-meter -X POST $IP:8080/init -H 'Content-Type: application/json' -d @test-cruntime-python/init.json
	for i in {1..3}
	do
		pretime
		curl --no-progress-meter --max-time 5 -X POST $IP:8080/run -H "Content-Type: application/json" -d @test-cruntime-python/run.json
		postime
		done
}

function stop_python {
	sudo $CRUNTIME_HOME/stop-vm -id $VMID
}

# TODO - create tap and destroy tap
# TODO - create init.json from source.

start_python #&> $tmpdir/python.log &

sleep 5

run_test_python #> $tmpdir/python.dat
stop_python
