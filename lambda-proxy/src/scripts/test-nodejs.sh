#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

# Clone git@github.com:apache/openwhisk-runtime-nodejs.git
NODEJS_ACTION_BASE=$HOME/git/openwhisk-runtime-nodejs/core/nodejsActionBase

# TODO - this depends on the function that is being used for the test.
function setup_node {
	cat test-nodejs/hello-world.js | jq -sR  '{value: {main: "main", code: .}}' > test-nodejs/init.json
}

function start_node {
	cd $NODEJS_ACTION_BASE
	nodejs app.js &
	echo $! > $tmpdir/node.pid
	wait
}

function run_test_node {
	curl --no-progress-meter -X POST localhost:8080/init -H 'Content-Type: application/json' -d @test-nodejs/init.json
	for i in {1..1000}
	do
		pretime
		curl --no-progress-meter -X POST localhost:8080/run -H 'Content-Type: application/json' -d @test-nodejs/run.json
		postime
		done
}

function stop_node {
	pid=`cat $tmpdir/node.pid`
	kill $pid
}

setup_node
start_node &> $tmpdir/nodejs.log &

sleep 1

run_test_node > $tmpdir/nodejs.dat
stop_node
