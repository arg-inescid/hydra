#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-benchmark.sh

# Processing input parameters
if [ "$#" -lt 3 ]; then
	echo "Syntax: <jvm|svm|niuk> <gv_java_hw|gv_javascript_hw|gv_python_hw> <test|benchmark> [<tests|concurrency> [<cpu> [<memory>]]]"
	exit 1
fi

backend=$1
app=$2
mode=$3

if [ "$#" -ge 4 ]; then
	workload=$4
else
	if [ "$mode" = "test" ]; then
		workload=10
	else
		workload=1
	fi
fi

if [ "$#" -ge 5 ]; then
	CPU=$5
fi

if [ "$#" -ge 6 ]; then
	MEM=$6
fi

echo "Running environment=$backend; app=$app; mode=$mode; workload=$workload; cpu=$CPU; mem=$MEM"

function benchmark {
	ab -p $APP_POST -T application/json -c $workload -n $((workload * 100))  http://$ip:8080/ &> $tmpdir/ab.log
}

function test {
	for i in $(seq 1 $workload)
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

# Adding firecracker to cgroup.
if [ ! -z "$CGROUP" ]
then
	PID=$(ps aux | grep firecracker | grep polyglot-proxy.img.socket | awk '{print $2}')
	echo "Adding $PID to cgroup $CGROUP"
	echo $PID | sudo tee -a /sys/fs/cgroup/$CGROUP/cgroup.procs
fi

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
RESULT_DIR=$BENCHMARKS_HOME/results/$APP_LANG/$APP_NAME-$backend-$mode-$workload-$CPU-$MEM
mkdir -p $RESULT_DIR
cp $tmpdir/lambda.* $tmpdir/*.log $RESULT_DIR
echo "Check logs: $RESULT_DIR/lambda.log"
