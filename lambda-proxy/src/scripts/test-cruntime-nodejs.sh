#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

CRUNTIME_HOME=$(DIR)/../../../lambda-manager/src/scripts/cruntime/

TAP=nodejstap
VMID=nodejsvm1

function start_node {
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/create_taps.sh $TAP $ip
	sudo $CRUNTIME_HOME/start-vm -ip $ip/$smask -gw $gateway -tap $TAP -id $VMID -img docker.io/openwhisk/action-nodejs-v12:latest
}

function run_test_node {
	curl --no-progress-meter --max-time 5 -X POST $ip:8080/init -H 'Content-Type: application/json' -d @test-cruntime-nodejs/init.json
	for i in {1..1000}
	do
		pretime
		curl --no-progress-meter --max-time 5 -X POST $ip:8080/run -H 'Content-Type: application/json' -d @test-cruntime-nodejs/run2.json
		postime
		done
}

function stop_node {
	sudo $CRUNTIME_HOME/stop-vm -id $VMID
	sudo bash $ARGO_HOME/lambda-manager/src/scripts/remove_taps.sh $TAP
}

cat hello-world.js | jq -sR  '{value: {main: "main", code: .}}' > test-cruntime-nodejs/init.json

start_node
sleep 5
run_test_node #> $tmpdir/nodejs.dat
stop_node
