#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

# Note: this file may have variables that need to be adapted to your local environment.

export JVMCI_CONFIG_CHECK=ignore
export RES_HOME=$DIR/../../../resources
export MANAGER_HOME=$DIR/../../../lambda-manager
export PROXY_HOME=$DIR/../../../lambda-proxy
export PROXY_JAR=$PROXY_HOME/build/libs/lambda-proxy-1.0-java.jar
export CODEBASE_HOME=$DIR/../../codebase
export TRUFFLE_HOME=$RES_HOME/truffle-build
export JAVA_HOME=$RES_HOME/graalvm-5fff260e25-java11-22.0.0-dev/
export KERNEL_PATH=$RES_HOME/vmlinux-4.14.35-1902.6.6.1.el7.container
export CRUNTIME_HOME=$DIR/cruntime
export VIRTUALIZE_PATH=$RES_HOME/virtualize.json
