#!/bin/bash

export SHARED=/tmp/shared
export JAVA_HOME=/graalvm-d219e07057-java11-21.1.0-dev

mkdir $SHARED
mount -t 9p -o trans=virtio,version=9p2000.L shared $SHARED
cd $SHARED
bash run.sh &> run.log
