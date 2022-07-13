#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh

TAP=benchtap
VMID=benchvm

function java_hw {
	IMG=docker.io/openwhisk/java8action:latest
	INIT_POST=test-cruntime-java/init.json
	RUN_POST=test-cruntime-java/run.json
}

function javascript_hw {
	IMG=docker.io/openwhisk/action-nodejs-v12:latest
	INIT_POST=test-cruntime-nodejs/init.json
	RUN_POST=test-cruntime-nodejs/run2.json
}

function python_hw {
	IMG=docker.io/openwhisk/action-python-v3.9:latest
	INIT_POST=test-cruntime-python/init.json
	RUN_POST=test-cruntime-python/run2.json
}

# Deleting old dat files
rm $tmpdir/{*.dat,*.log,*.png} &> /dev/null

# Load function to benchmark
javascript_hw
#java_hw
#python_hw

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

for c in 1
do
	echo "Running with $c concurrent isolates..."
	ab -p $RUN_POST -T application/json -s 30 -c $c -n $((c * 10000))  http://$ip:8080/run &> $tmpdir/ab-$c.log
	cat $tmpdir/ab-$c.log | grep "Time per request" | grep "(mean)" | awk '{print $4}' >> $tmpdir/ab-latency.dat
	cat $tmpdir/ab-$c.log | grep Requests | awk '{print $4}'  >> $tmpdir/ab-tput.dat
	cat $tmpdir/ab-$c.log | grep Concurrency | awk '{print $3}'  >> $tmpdir/ab-concurrency.dat
	echo "Running with $c concurrent isolates... done!"
done

# Stopping VM.
sudo $CRUNTIME_HOME/stop-vm -id $VMID
sudo bash $ARGO_HOME/lambda-manager/src/scripts/remove_taps.sh $TAP

# Wait for log_rss.
wait
