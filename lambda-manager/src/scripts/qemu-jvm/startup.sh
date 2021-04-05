#!/bin/bash

mkdir /tmp/shared
mount -t 9p -o trans=virtio,version=9p2000.L shared /tmp/shared
bash /tmp/shared/run.sh
