#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

export RES_HOME=$DIR/../../../resources
export MANAGER_HOME=$DIR/../../../lambda-manager
export PROXIES_HOME=$DIR/../../../lambda-proxies
export CODEBASE_HOME=$DIR/../../codebase
export JAVA_HOME=$RES_HOME/graalvm-ni-ee-java11-21.3.0-dev
export KERNEL_PATH=$RES_HOME/vmlinux-4.14.35-1902.6.6.1.el7.container
export VIRTUALIZE_PATH=$RES_HOME/virtualize.json
