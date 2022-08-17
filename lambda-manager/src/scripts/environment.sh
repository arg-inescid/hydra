#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

# Note: this file may have variables that need to be adapted to your local environment.

export RES_HOME=$DIR/../../../resources
export MANAGER_HOME=$DIR/../../../lambda-manager
export NIUK_HOME=$DIR/../../../niuk
export BENCHMARKS_HOME=$DIR/../../../benchmarks
export PROXY_HOME=$DIR/../../../lambda-proxy
export PROXY_JAR=$PROXY_HOME/build/libs/lambda-proxy-1.0-all.jar
export CODEBASE_HOME=$DIR/../../codebase
export GRAALVISOR_HOME=$PROXY_HOME/build/native-image
export JAVA_HOME=/home/rbruno/software/graalvm-ee-java11-22.1.0
# TODO - the KERNEL_PATH option is deprecated and will be removed.
export KERNEL_PATH=$RES_HOME/vmlinux-4.14.35-1902.6.6.1.el7.container
export CRUNTIME_HOME=$DIR/cruntime
