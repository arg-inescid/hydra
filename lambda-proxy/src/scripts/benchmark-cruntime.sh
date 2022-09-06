#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-benchmark.sh

if [ "$#" -lt 2 ]; then
	echo "Syntax: <cr_java_hw|cr_javascript_hw|cr_python_hw> <test|benchmark> [<tests|concurrency> [<cpu> [<memory>]]]"
	exit 1
fi

app=$1
mode=$2

if [ "$#" -ge 3 ]; then
	workload=$3
else
	if [ "$mode" = "test" ]; then
		workload=10
	else
		workload=1
	fi
fi

if [ "$#" -ge 4 ]; then
	CPU=$4
fi

if [ "$#" -ge 5 ]; then
	MEM=$5
fi

echo "Running app=$app; mode=$mode; workload=$workload; cpu=$CPU; mem=$MEM"

function benchmark {
	ab -p $RUN_POST -T application/json -s 60 -c $workload -n $((workload * 100))  http://$ip:8080/run &> $tmpdir/ab.log
}

function test {
	for i in $(seq 1 $workload)
	do
		pretime
		curl -s --max-time 60 -X POST $ip:8080/run -H 'Content-Type: application/json' -d @$RUN_POST
		postime
	done
}

TAP=benchtap
VMID=benchvm

# Deleting old dat files
rm $tmpdir/{*.dat,*.log,*.png} &> /dev/null

# Load function to benchmark
$app

# Create tap.
sudo bash $MANAGER_HOME/src/scripts/create_taps.sh $TAP $ip

# Launch runtime.
sudo $CRUNTIME_HOME/start-vm -ip $ip/$smask -gw $gateway -tap $TAP -id $VMID -img $IMG -mem $MEM -cpu $CPU

# Just let the VM boot...
sleep 5

# Adding firecracker to cgroup.
if [ ! -z "$CGROUP" ]
then
	PID=$(ps aux | grep firecracker | grep $VMID | awk '{print $2}')
	echo "Adding $PID to cgroup $CGROUP"
	echo $PID | sudo tee -a /sys/fs/cgroup/$CGROUP/cgroup.procs
fi

# Log memory.	
log_rss $(ps aux | grep firecracker | grep $VMID | awk '{print $2}') $tmpdir/lambda.rss &

# Load function to benchmark
curl -s -X POST $ip:8080/init -H 'Content-Type: application/json' -d @$INIT_POST

# Run test/benchmark.
$mode | tee -a $tmpdir/app.log

# Stopping VM.
sudo $CRUNTIME_HOME/stop-vm -id $VMID
sudo bash $MANAGER_HOME/src/scripts/remove_taps.sh $TAP

# Wait for log_rss.
wait

# Copy output to app's privde result dir.
RESULT_DIR=$BENCHMARKS_HOME/results/$APP_LANG/$APP_NAME-$mode-$workload-$CPU-$MEM
mkdir -p $RESULT_DIR
cp $tmpdir/lambda.* $tmpdir/*.log $RESULT_DIR
echo "Check logs: $RESULT_DIR/lambda.log"
