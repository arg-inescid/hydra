#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

export app_dir=$(pwd)
bash $(DIR)/../../../graalvisor/graalvisor-gdb
