#!/bin/bash

# Getting the local default ip used to connect to the internet.
IP=$(ip route get 8.8.8.8 | grep -oP  'src \K\S+')
PORT=8000

function DIR {
   echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function gv_java_hw {
    APP_LANG=java
    APP_NAME=gv-hello-world
    APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libhelloworld.so
    APP_MAIN=com.hello_world.HelloWorld
    curl -s -X POST $ip:8080/register?name=hw\&entryPoint=$APP_MAIN\&language=$APP_LANG\&sandbox=$SANDBOX\&cpuCgroupQuota=100000 -H 'Content-Type: application/json' --data-binary @$APP_SO
    echo '{"name":"hw","async":"false","cached":"false","arguments":""}' > $APP_POST
}

function gv_java_filehashing {
    APP_LANG=java
    APP_NAME=gv-file-hashing
    APP_MAIN=com.filehashing.FileHashing
    APP_SO=$BENCHMARKS_HOME/src/$APP_LANG/$APP_NAME/build/libfilehashing.so
    curl -s -X POST $ip:8080/register?name=filehashing\&entryPoint=$APP_MAIN\&language=$APP_LANG\&sandbox=$SANDBOX\&cpuCgroupQuota=50000 -H 'Content-Tsype: application/json' --data-binary @$APP_SO
    echo '{"name":"filehashing","async":"false","cached":"true","arguments":"{\"url\":\"http://'$IP':'$PORT'/blob\"}"}' > $APP_POST
}

function test_java_hw {
    # Writing post file to disk
    mkdir $tmpdir/hw &> /dev/null
    APP_POST=$tmpdir/hw/payload.post
    gv_java_hw
    for i in $(seq 1 $number_of_tests)
    do
        pretime
        curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d $(cat $APP_POST)
        postime
    done
}

function test_java_fh {
    # Writing post file to disk
    mkdir $tmpdir/fh &> /dev/null
    APP_POST=$tmpdir/fh/payload.post
    gv_java_filehashing
    for i in $(seq 1 $number_of_tests)
    do
        pretime
        curl -s -X POST $ip:8080 -H 'Content-Type: application/json' -d $(cat $APP_POST)
        postime
    done
}

source $(DIR)/shared.sh

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters"
    exit 1
else
    number_of_tests=$1
fi

# Setting a sandbox if not already set.
if [ -z "$SANDBOX" ]
then
    export SANDBOX=isolate
fi

# Preparing working directory
sudo rm -r $tmpdir/ &> /dev/null
mkdir $tmpdir &> /dev/null

# Setting up environment.
ip=127.0.0.1
start_svm &> $tmpdir/lambda.log &

# Let the lambda start.
wait_port $ip 8080

PID=$(cat $tmpdir/lambda.pid)

# Write lambda pid to file.
# echo -n "$PID" > $tmpdir/lambda.pid ############### Do I need this?

# Log memory.
# log_rss $PID $tmpdir/lambda.rss & ############### Do I need this?

# Run test/benchmark.
test_java_hw &> $tmpdir/hello_world.log &
HW_PID=$!
#test_java_fh &> $tmpdir/file_hashing.log &
#FH_PID=$!

wait $HW_PID
#wait $FH_PID

# Teardown the lambda.
stop_svm

# Remove main cgroup
start=$(date +%s%N)
rmdir /sys/fs/cgroup/user.slice/user-1000.slice/isolate/cgroup-*
rmdir /sys/fs/cgroup/user.slice/user-1000.slice/isolate
end=$(date +%s%N)
echo "Removing cgroup took $((($end - $start)/1000)) us"

python main.py $tmpdir/lamda.log "process time" "hwProcessTime"
python main.py $tmpdir/lamda.log "Time taken" "hwTimeTaken"
python main.py $tmpdir/hello_world.log "New" "newCgroupTimes"
python main.py $tmpdir/hello_world.log "Inserted" "insertCgroupTimes"
python main.py $tmpdir/hello_world.log "Removed" "removeCgroupTimes"
