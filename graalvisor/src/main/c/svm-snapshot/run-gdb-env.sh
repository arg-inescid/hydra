#!/bin/sh

# You can add any default options here
UNSHARE_OPTS="--kill-child --map-root-user --keep-caps --mount-proc -f -p"
ARCH_OPTS="-R"
HYDRALLOC="$ARGO_HOME/graalvisor/src/main/c/svm-snapshot/deps/dlmalloc/hydralloc.so"

# Run the command with all arguments passed to the script
unshare $UNSHARE_OPTS \
setarch $ARCH_OPTS \
gdb -x gdb-commands.txt -ex "set environment LD_PRELOAD $HYDRALLOC" --args \
$@
