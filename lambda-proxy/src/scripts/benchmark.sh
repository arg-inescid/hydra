#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

ip=127.0.0.1

# Setting up VM environment.
setup_polyglot_svm

# Starting VM.
start_polyglot_svm &> $tmpdir/lambda.log &

# Just let the VM boot...
sleep 1

# Loading hello world configuration
polyglot_java_hello_world

# Registering hello world function
curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG -H 'Content-Type: application/json' --data-binary @$APP_SO

# Writing post file to disk
APP_POST=$tmpdir/payload.post
echo '{"name":"hw","async":"false","arguments":""}' > $APP_POST

# Deleting old dat files
rm $tmpdir/*.dat

#for c in 1 2 4 8 16 32 64 128 256 512 1024
for c in 1 2 4 8 16 32
do
	echo "Running with $c concurrent isolates..."
	ab -p $APP_POST -T application/json -c $c -n $((c * 25000))  http://$ip:8080/ &> $tmpdir/ab-$c.log
	cat $tmpdir/ab-$c.log | grep "Time per request" | grep "(mean)" | awk '{print $4}' > ab-latency.dat
	cat $tmpdir/ab-$c.log | grep Requests | awk '{print $4}'  > ab-tput.dat
	cat $tmpdir/ab-$c.log | grep Concurrency | awk '{print $3}'  > ab-concurrency.dat
	echo "Running with $c concurrent isolates... done!"
done

# Stopping VM.
stop_baremetal &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"

# Average latency (ms)
cat /tmp/test-proxy/ab-* | grep "Time per request" | grep "(mean)" | awk '{print $4}'

# Throuhgput (ops/s)
cat /tmp/test-proxy/ab-* | grep Requests | awk '{print $4}'
