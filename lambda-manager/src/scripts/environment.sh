#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
	echo "ARGO_HOME is not defined. Exiting..."
	exit 1
fi

if [[ -z "${JAVA_HOME}" ]]; then
	echo "JAVA_HOME is not defined. Exiting..."
	exit 1
fi

# Note: this file may have variables that need to be adapted to your local environment.
export RES_HOME=$ARGO_HOME/resources
export MANAGER_HOME=$ARGO_HOME/lambda-manager
export BENCHMARKS_HOME=$ARGO_HOME/benchmarks
export PROXY_HOME=$ARGO_HOME/graalvisor
export PROXY_JAR=$PROXY_HOME/build/libs/graalvisor-1.0-all.jar
export CODEBASE_HOME=$MANAGER_HOME/codebase
export GRAALVISOR_HOME=$PROXY_HOME/build/native-image
export CRUNTIME_HOME=$MANAGER_HOME/src/scripts/cruntime
export PGO_FILES=$CODEBASE_HOME
