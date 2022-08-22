#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-benchmark.sh

if [ "$#" -ne 3 ]; then
	echo "Syntax: <jvm|svm|niuk> <java_hw|java_sleep>"
	exit 1
else
	backend=$1
	app=$2
	mode=$3
fi

ip=127.0.0.1

function setup_java_baremetal {
	mkdir $tmpdir &> /dev/null
	sudo chown -R $USER:$USER $tmpdir
}

function start_java_baremetal {
	proxy_args="$(date +%s%N | cut -b1-13) $APP_MAIN 8080"
	proxy_main=org.graalvm.argo.lambda_proxy.BaremetalJavaProxy
	start_jvm
}

function setup_java_svm {
	mkdir $tmpdir &> /dev/null
	sudo ls $tmpdir &> /dev/null
	cd $tmpdir
	$JAVA_HOME/bin/native-image \
		-cp $PROXY_JAR:$APP_JAR \
		-H:ReservedAuxiliaryImageBytes=0 \
		-H:-UseCompressedReferences \
		--features=org.graalvm.argo.lambda_proxy.engine.JavaEngineSingletonFeature \
		org.graalvm.argo.lambda_proxy.BaremetalJavaProxy \
		app \
		-H:ConfigurationFileDirectories=$APP_CONFIG
}

function start_java_svm {
	proxy_args="$(date +%s%N | cut -b1-13) $APP_MAIN 8080"
	start_svm
}

# TODO - merge with setup_java_svm.
function setup_java_niuk {
	mkdir $tmpdir &> /dev/null
	sudo ls $tmpdir &> /dev/null
	cd $tmpdir
	$JAVA_HOME/bin/native-image \
		-cp $PROXY_JAR:$APP_JAR \
		-H:ReservedAuxiliaryImageBytes=0 \
		-H:-UseCompressedReferences \
		--features=org.graalvm.argo.lambda_proxy.engine.JavaEngineSingletonFeature \
		org.graalvm.argo.lambda_proxy.JavaProxy \
		app \
		-H:ConfigurationFileDirectories=$APP_CONFIG
	$NIUK_HOME/build_niuk.sh app app.img
}

function start_java_niuk {
	proxy_args="lambda_timestamp=$(date +%s%N | cut -b1-13) lambda_entry_point=$APP_MAIN lambda_port=8080"
	start_niuk
}

function run_test_java {
	for i in {1..10}
	do
		pretime
		curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d @$APP_POST
		postime
	done
}

# Pick the application to test/benchmark
$app

if [ "$backend" == "jvm" ]; then
	setup_java_baremetal
	start_java_baremetal &> $tmpdir/lambda.log &
elif [ "$backend" == "svm" ]; then
	setup_java_svm
	start_java_svm &> $tmpdir/lambda.log &
elif [ "$backend" == "niuk" ]; then
	setup_java_niuk
	start_java_niuk &> $tmpdir/lambda.log &
fi

sleep 5

run_test_java

# Stop baremetal works for both jvm and svm
if [ "$backend" == "jvm" ]; then
	stop_baremetal &>> $tmpdir/lambda.log
elif [ "$backend" == "svm" ]; then
	stop_baremetal &>> $tmpdir/lambda.log
elif [ "$backend" == "niuk" ]; then
	stop_niuk &>> $tmpdir/lambda.log
fi
echo "Check logs: $tmpdir/lambda.log"
