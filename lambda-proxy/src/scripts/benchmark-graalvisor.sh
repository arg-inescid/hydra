#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-benchmark.sh

if [ "$#" -ne 3 ]; then
	echo "Syntax: <jvm|svm|niuk> <gv_java_hw|gv_javascript_hw|gv_python_hw> <test|benchmark>"
	exit 1
else
	backend=$1
	app=$2
	mode=$3
fi

function benchmark {
	for c in 1 2
	do
		echo "Running with $c concurrent isolates..."
		ab -p $APP_POST -T application/json -c $c -n $((c * 5000))  http://$ip:8080/ &> $tmpdir/ab-$c.log
		cat $tmpdir/ab-$c.log | grep "Time per request" | grep "(mean)" | awk '{print $4}' >> $tmpdir/ab-latency.dat
		cat $tmpdir/ab-$c.log | grep Requests | awk '{print $4}'  >> $tmpdir/ab-tput.dat
		cat $tmpdir/ab-$c.log | grep Concurrency | awk '{print $3}'  >> $tmpdir/ab-concurrency.dat
		echo "Running with $c concurrent isolates... done!"
	done
}

function test {
	for i in {1..10}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d $(cat $APP_POST)
		postime
	done
}

# Writing post file to disk
APP_POST=$tmpdir/payload.post

# Deleting old dat and log files
rm $tmpdir/{*.dat,*.log} &> /dev/null

# Setting up environment.
if [ "$backend" == "jvm" ]; then
	ip=127.0.0.1
	# TODO - jvm does not support so apps (Java built as Native Library). We should instead send a Jar.
	start_polyglot_jvm &> $tmpdir/lambda.log &
elif [ "$backend" == "svm" ]; then
	ip=127.0.0.1
	setup_polyglot_svm
	start_polyglot_svm &> $tmpdir/lambda.log &
elif [ "$backend" == "niuk" ]; then
	# Note: ip is already set when loading test-shared.sh
	setup_polyglot_niuk
	start_polyglot_niuk &> $tmpdir/lambda.log &
	# Let niuk boot...
	sleep 1 
	log_rss $(ps aux | grep firecracker | grep polyglot-proxy.img.socket | awk '{print $2}') $tmpdir/lambda.rss & # TODO - move to start_polyglot_niuk
fi

# Let graalvisor start.
sleep 1 

# Load function into runtime.
$app

# Run test/benchmark.
$mode | tee -a $tmpdir/app.log

# Teardown environment.
if [ "$backend" == "jvm" ]; then
	stop_baremetal &>> $tmpdir/lambda.log
elif [ "$backend" == "svm" ]; then
	stop_baremetal &>> $tmpdir/lambda.log
elif [ "$backend" == "niuk" ]; then
	stop_niuk &>> $tmpdir/lambda.log
fi
wait

# Copy output to app's privde result dir.
RESULT_DIR=$BENCHMARKS_HOME/results/$APP_LANG/$APP_NAME-$backend
mkdir -p $RESULT_DIR
cp $tmpdir/lambda.* $tmpdir/app.log $RESULT_DIR
echo "Check logs: $RESULT_DIR/lambda.log"
