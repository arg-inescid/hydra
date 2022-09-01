#!/bin/bash

function DIR {
	echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

# Done
GV_BENCHMARKS="$GV_BENCHMARKS gv_java_hw"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_hw"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_python_hw"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_java_hw"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_hw"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_python_hw"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_java_sleep"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_python_sleep" # TODO - weird sleep over
#GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_sleep"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_java_sleep"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_python_sleep"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_sleep"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_java_filehashing"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_java_filehashing"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_dynamichtml"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_dynamichtml"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_python_dynamichtml"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_python_dynamichtml"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_python_thumbnail"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_python_thumbnail"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_uploader"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_uploader"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_java_httprequest"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_java_httprequest"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_java_videoprocessing"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_java_videoprocessing"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_python_uploader"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_python_uploader"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_python_compression"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_python_compression"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_python_videoprocessing"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_python_videoprocessing"
#GV_BENCHMARKS="$GV_BENCHMARKS gv_javascript_thumbnail"
#CR_BENCHMARKS="$CR_BENCHMARKS cr_javascript_thumbnail"

for benchmark in $GV_BENCHMARKS; do $(DIR)/benchmark-graalvisor.sh niuk $benchmark test; done
#for benchmark in $GV_BENCHMARKS; do $(DIR)/benchmark-graalvisor.sh svm $benchmark test; done
for benchmark in $CR_BENCHMARKS; do $(DIR)/benchmark-cruntime.sh $benchmark test; done
