#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh

ip=127.0.0.1

function java_hw {
	polyglot_java_hello_world
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function javascript_hw {
	polyglot_javascript_hello_world
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' -d @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}

function python_hw {
	polyglot_python_hello_world
	curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' -d @$APP_SCRIPT
	echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST
}


# Writing post file to disk
APP_POST=$tmpdir/payload.post

# Deleting old dat files
rm $tmpdir/*.dat &> /dev/null

# Setting up VM environment.
setup_polyglot_svm

# Starting VM.
start_polyglot_svm &> $tmpdir/lambda.log &

# Just let the VM boot...
sleep 1

# Load function to benchmark
#javascript_hw
java_hw
#python_hw

#for c in 1 2 4 8 16 32 64 128 256 512 1024
for c in 1 2 4 8
do
	echo "Running with $c concurrent isolates..."
	ab -p $APP_POST -T application/json -c $c -n $((c * 50000))  http://$ip:8080/ &> $tmpdir/ab-$c.log
	cat $tmpdir/ab-$c.log | grep "Time per request" | grep "(mean)" | awk '{print $4}' >> $tmpdir/ab-latency.dat
	cat $tmpdir/ab-$c.log | grep Requests | awk '{print $4}'  >> $tmpdir/ab-tput.dat
	cat $tmpdir/ab-$c.log | grep Concurrency | awk '{print $3}'  >> $tmpdir/ab-concurrency.dat
	echo "Running with $c concurrent isolates... done!"
done

# Stopping VM.
stop_baremetal &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"

# Plotting.
paste -d ' ' $tmpdir/ab-concurrency.dat $tmpdir/ab-latency.dat | ~/git/helper-scripts/plotting/plotter.py -x Concurrency -y 'Mean Latency (ms)' -o $tmpdir/latency.png
paste -d ' ' $tmpdir/ab-concurrency.dat $tmpdir/ab-tput.dat    | ~/git/helper-scripts/plotting/plotter.py -x Concurrency -y Throughput -o $tmpdir/tput.png
cat $tmpdir/lambda.rss | ~/git/helper-scripts/plotting/plotter.py -y 'Memory (KBs)' -o $tmpdir/mem.png
