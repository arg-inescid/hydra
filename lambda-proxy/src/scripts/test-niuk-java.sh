#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/test-shared.sh
source $(DIR)/test-shared.local

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
		-H:Virtualize=$VIRTUALIZE_PATH \
		-H:ConfigurationFileDirectories=$APP_CONFIG
}

function start_java_niuk {
	proxy_args="lambda_timestamp=$(date +%s%N | cut -b1-13) lambda_entry_point=$APP_MAIN lambda_port=8080"
	start_niuk
}

setup_java_niuk
start_java_niuk &> $tmpdir/lambda.log &
sleep 5
#run_test_java
run_workload
stop_niuk &>> $tmpdir/lambda.log
echo "Check logs: $tmpdir/lambda.log"
