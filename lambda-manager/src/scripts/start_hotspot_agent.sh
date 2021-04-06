#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
source $DIR/env.sh

LAMBDA_NAME=$1
if [ -z "$LAMBDA_NAME" ]
then
	echo "Lambda name is not present."
	exit 1
fi

LAMBDA_HOME=$MANAGER_HOME/src/lambdas/$LAMBDA_NAME
LAMBDA_JAR=$LAMBDA_HOME/$LAMBDA_NAME.jar

$JAVA_HOME/bin/java \
	-Djava.library.path=$JAVA_HOME/lib \
	-agentlib:native-image-agent=config-output-dir=$LAMBDA_HOME/config,caller-filter-file=$MANAGER_HOME/src/main/resources/caller-filter-config.json \
	-jar $LAMBDA_JAR \
	${@:2}
