#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

GV_BENCHMARKS="$GV_BENCHMARKS gv_java_hw"                 # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_hw"           # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_python_hw"               # 256 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_java_hw"                 # 256 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_hw"           # 256 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_python_hw"               # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_java_sleep"
GV_BENCHMARKS="$GV_BENCHMARKS gv_python_sleep"
GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_sleep"
CR_BENCHMARKS="$CR_BENCHMARKS cr_java_sleep"
CR_BENCHMARKS="$CR_BENCHMARKS cr_python_sleep"
CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_sleep"
GV_BENCHMARKS="$GV_BENCHMARKS gv_java_filehashing"        # 256 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_java_filehashing"        # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_dynamichtml"  # 256 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_dynamichtml"  # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_python_dynamichtml"      # 512 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_python_dynamichtml"      # 512 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_python_thumbnail"        # 512 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_python_thumbnail"        # 512 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_uploader"     # 256 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_uploader"     # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_java_httprequest"        # 256 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_java_httprequest"        # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_java_videoprocessing"    # 1024 MB, note reduce benchmark to 10*workload
CR_BENCHMARKS="$CR_BENCHMARKS cr_java_videoprocessing"    # 1024 MB, note reduce benchmark to 10*workload
GV_BENCHMARKS="$GV_BENCHMARKS gv_python_uploader"         # 512 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_python_uploader"         # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_python_compression"      # 1024 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_python_compression"      # 256 MB
GV_BENCHMARKS="$GV_BENCHMARKS gv_python_videoprocessing"  # 512 MB, note reduce benchmark to 10*workload
CR_BENCHMARKS="$CR_BENCHMARKS cr_python_videoprocessing"  # 512 MB, note reduce benchmark to 10*workload
GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_thumbnail"    # 512 MB
CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_thumbnail"    # 512 MB

function cdf_latency_filehashing {
    $(DIR)/benchmark-cruntime.sh        cr_java_filehashing test
    $(DIR)/benchmark-graalvisor.sh niuk gv_java_filehashing test
}

function warm_latency {
    for benchmark in $GV_BENCHMARKS; do $(DIR)/benchmark-graalvisor.sh niuk $benchmark test; done
    for benchmark in $CR_BENCHMARKS; do $(DIR)/benchmark-cruntime.sh        $benchmark test; done
}

# Memory (fixed HW resources of 1 core and 2GB of memory, measure ops/s/mb)
function memory {

    # Run graalvisor with 1 to 8 concurrent requests -> measure tput and RSS
    function memory_gv {
        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_java_hw benchmark 8 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_python_hw benchmark 8 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_javascript_hw benchmark 8 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_java_filehashing benchmark 8 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_javascript_dynamichtml benchmark 8 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_python_dynamichtml benchmark 4 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_python_thumbnail benchmark 4 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_javascript_uploader benchmark 8 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_java_httprequest benchmark 8 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_java_videoprocessing benchmark 2 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_python_uploader benchmark 4 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_python_compression benchmark 2 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_python_videoprocessing benchmark 4 1 2048

        echo "100000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # 1 core
        $(DIR)/benchmark-graalvisor.sh niuk gv_javascript_thumbnail benchmark 4 1 2048
    }

    # Run custom runtime with 1 to 8 concurrent requests
    function memory_cr {
        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_java_hw benchmark 1 1 256

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_python_hw benchmark 1 1 256

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_javascript_hw benchmark 1 1 256

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_java_filehashing benchmark 1 1 256

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_javascript_dynamichtml benchmark 1 1 256

        echo "25000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .25 cores
        $(DIR)/benchmark-cruntime.sh cr_python_dynamichtml benchmark 1 1 512

        echo "25000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .25 cores
        $(DIR)/benchmark-cruntime.sh cr_python_thumbnail benchmark 1 1 512

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_javascript_uploader benchmark 1 1 256

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_java_httprequest benchmark 1 1 256

        echo "50000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .5 cores
        $(DIR)/benchmark-cruntime.sh cr_java_videoprocessing benchmark 1 1 1024

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_python_uploader benchmark 1 1 256

        echo "12500 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .125 cores
        $(DIR)/benchmark-cruntime.sh cr_python_compression benchmark 1 1 256

        echo "25000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .25 cores
        $(DIR)/benchmark-cruntime.sh cr_python_videoprocessing benchmark 1 1 512

        echo "25000 100000" | sudo tee -a /sys/fs/cgroup/$CGROUP/cpu.max # .25 cores
        $(DIR)/benchmark-cruntime.sh cr_javascript_thumbnail benchmark 1 1 512
    }

    export CGROUP="experiments"

    # Create cgroup.
    #sudo mkdir /sys/fs/cgroup/experiments/

    memory_gv

    memory_cr

    # To remove cgroup.
    #sudo rmdir /sys/fs/cgroup/Example

    # Clear variable.
    unset CGROUP
}

#cdf_latency_filehashing
#warm_latency
#memory

