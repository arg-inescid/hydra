#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

export app_dir=$(DIR)
bash $(DIR)/../../../graalvisor/graalvisor-gdb
