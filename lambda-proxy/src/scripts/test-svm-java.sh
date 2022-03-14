#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

ip=127.0.0.1

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


setup_java_svm
start_java_svm &> $tmpdir/lambda.log &
sleep 5
run_test_java
run_workload_java
stop_baremetal &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"
