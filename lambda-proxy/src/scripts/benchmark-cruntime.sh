#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-benchmark.sh

function benchmark {
	for c in 1
	do
		echo "Running with $c concurrent isolates..."
		ab -p $RUN_POST -T application/json -s 30 -c $c -n $((c * 1000))  http://$ip:8080/run &> $tmpdir/ab-$c.log
		cat $tmpdir/ab-$c.log | grep "Time per request" | grep "(mean)" | awk '{print $4}' >> $tmpdir/ab-latency.dat
		cat $tmpdir/ab-$c.log | grep Requests | awk '{print $4}'  >> $tmpdir/ab-tput.dat
		cat $tmpdir/ab-$c.log | grep Concurrency | awk '{print $3}'  >> $tmpdir/ab-concurrency.dat
		echo "Running with $c concurrent isolates... done!"
	done
}

function test {
	for i in {1..3}
	do
		pretime
		curl --no-progress-meter --max-time 60 -X POST $ip:8080/run -H 'Content-Type: application/json' -d @$RUN_POST
		postime
	done
}

if [ "$#" -ne 2 ]; then
	echo "Syntax: <cr_java_hw|cr_javascript_hw|cr_python_hw> <test|benchmark>"
	exit 1
else
	app=$1
	mode=$2
fi

TAP=benchtap
VMID=benchvm

# Deleting old dat files
rm $tmpdir/{*.dat,*.log,*.png} &> /dev/null

# Load function to benchmark
$app

# Create tap.
sudo bash $ARGO_HOME/lambda-manager/src/scripts/create_taps.sh $TAP $ip

# Launch runtime.
sudo $CRUNTIME_HOME/start-vm -ip $ip/$smask -gw $gateway -tap $TAP -id $VMID -img $IMG

# Just let the VM boot...
sleep 5

# Log memory.	
log_rss $(ps aux | grep firecracker | grep $VMID | awk '{print $2}') $tmpdir/lambda.rss &

# Load function to benchmark
curl -s -X POST $ip:8080/init -H 'Content-Type: application/json' -d @$INIT_POST

# Run test/benchmark.
$mode

# Stopping VM.
sudo $CRUNTIME_HOME/stop-vm -id $VMID
sudo bash $ARGO_HOME/lambda-manager/src/scripts/remove_taps.sh $TAP

# Wait for log_rss.
wait
