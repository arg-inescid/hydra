#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function cidr_to_netmask() {
    value=$(( 0xffffffff ^ ((1 << (32 - $1)) - 1) ))
    echo "$(( (value >> 24) & 0xff )).$(( (value >> 16) & 0xff )).$(( (value >> 8) & 0xff )).$(( value & 0xff ))"
}

function next_ip(){
    IP_HEX=$(printf '%.2X%.2X%.2X%.2X\n' `echo $1 | sed -e 's/\./ /g'`)
    NEXT_IP_HEX=$(printf %.8X `echo $(( 0x$IP_HEX + 1 ))`)
    NEXT_IP=$(printf '%d.%d.%d.%d\n' `echo $NEXT_IP_HEX | sed -r 's/(..)/0x\1 /g'`)
    echo "$NEXT_IP"
}

source $(DIR)/../../../lambda-manager/src/scripts/environment.sh

tmpdir=/tmp/test-proxy
mkdir $tmpdir &> /dev/null

# TODO - make this a function?
# Network setup for the test. Gateway is the ip of the host. The guest will have the next ip.
gateway=$(ip route get 8.8.8.8 | grep -oP  'src \K\S+')
smask=$(ip r | grep $gateway | awk '{print $1}' | awk -F / '{print $2}')
mask=$(cidr_to_netmask $smask)
ip=$(next_ip $gateway)

# Default memory and cpu count.
MEM=1024
CPU=1

function pretime {
	ts=$(date +%s%N)
}

function postime {
	tt=$((($(date +%s%N) - $ts)/1000))
	printf "\nTime taken: $tt us\n"
}

function log_rss {
	PID=$1
	OFILE=$2
	rm $OFILE
        while sudo kill -0 $PID &> /dev/null; do
                ps -q $PID -o rss= >> $OFILE
                sleep .5
        done
}

function stop_niuk {
	ppid=`sudo cat $tmpdir/lambda.pid`
	for child in $(ps -o pid --no-headers --ppid $ppid); do
		sudo kill $child 
	done
	sudo bash $MANAGER_HOME/src/scripts/remove_taps.sh testtap
	sudo rm -f $tmpdir/*.socket
}

function stop_baremetal {
	pid=`sudo cat $tmpdir/lambda.pid`
	sudo kill $pid
}

function start_niuk {
	cd $tmpdir
	sudo bash $MANAGER_HOME/src/scripts/create_taps.sh testtap $ip
	sudo bash $NIUK_HOME/run_niuk.sh \
		--vmm firecracker \
		--disk $tmpdir/polyglot-proxy.img \
		--kernel $RES_HOME/hello-vmlinux.bin \
		--memory $MEM \
		--cpu $CPU \
		--ip $ip \
		--gateway $gateway \
		--mask $mask \
		--tap testtap \
		--console \
		$proxy_args 
}

function start_polyglot_niuk {
	proxy_args="lambda_timestamp=$(date +%s%N | cut -b1-13) lambda_port=8080 LD_LIBRARY_PATH=/lib:/lib64:/apps:/usr/local/lib JAVA_HOME=/jvm"
	start_niuk
}

function start_svm {
	cd $tmpdir
	./app &
	pid=$!
	echo $! > $tmpdir/lambda.pid
	log_rss $pid $tmpdir/lambda.rss &
	wait
}

function start_jvm {
	#NI_AGENT="-agentlib:native-image-agent=config-output-dir=$tmpdir/agent-output"
	$JAVA_HOME/bin/java $NI_AGENT -cp $PROXY_JAR:$APP_JAR $proxy_main &
	pid=$!
	echo $! > $tmpdir/lambda.pid
	log_rss $pid $tmpdir/lambda.rss &
	wait
}

function setup_polyglot_svm {
	mkdir $tmpdir &> /dev/null
	sudo ls $tmpdir &> /dev/null
	cp $GRAALVISOR_HOME/polyglot-proxy $tmpdir/app
}

function setup_polyglot_niuk {
	mkdir $tmpdir &> /dev/null
	sudo ls $tmpdir &> /dev/null
	cp $GRAALVISOR_HOME/polyglot-proxy.img $tmpdir
}

function start_polyglot_svm {
	export lambda_timestamp="$(date +%s%N | cut -b1-13)"
	export lambda_port="8080"
	start_svm
}

function start_polyglot_jvm {
	export lambda_timestamp="$(date +%s%N | cut -b1-13)"
	export lambda_port="8080"
	export lambda_entry_point="$APP_MAIN"
	export proxy_main="org.graalvm.argo.lambda_proxy.PolyglotProxy"
	start_jvm
}

