#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh

CRUNTIME_HOME=$(DIR)/../../../lambda-manager/src/scripts/cruntime/

IP=10.0.0.165
MASK=16
GW=10.0.0.201
TAP=tap001
VMID=nodevm5

function start_node {
	sudo $CRUNTIME_HOME/start-vm -ip $IP/$MASK -gw $GW -tap $TAP -id $VMID -img docker.io/openwhisk/action-nodejs-v12:latest
}

function run_test_node {
	curl --no-progress-meter -X POST $IP:8080/init -H 'Content-Type: application/json' -d @test-cruntime-nodejs/init.json
	for i in {1..3}
	do
		pretime
		curl --no-progress-meter --max-time 5 -X POST $IP:8080/run -H 'Content-Type: application/json' -d @test-cruntime-nodejs/run.json
		postime
		done
}

function stop_node {
	sudo $CRUNTIME_HOME/stop-vm -id $VMID
}

# TODO - create tap and destroy tap
cat hello-world.js | jq -sR  '{value: {main: "main", code: .}}' > test-cruntime-nodejs/init.json

# Just to cache sudo permissions.
sudo ls > /dev/null

start_node #&> $tmpdir/nodejs.log

sleep 5

run_test_node #> $tmpdir/nodejs.dat
stop_node
