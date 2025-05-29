#!/bin/sh

# You can add any default options here
UNSHARE_OPTS="--kill-child --map-root-user --keep-caps --mount-proc -f -p"
ARCH_OPTS="-R"

# Run the command with all arguments passed to the script
#LD_PRELOAD=./deps/dlmalloc/newdl.so exec unshare $UNSHARE_OPTS setarch $ARCH_OPTS "$@"
#exec unshare $UNSHARE_OPTS setarch $ARCH_OPTS env LD_PRELOAD=./deps/dlmalloc/newdl.so "$@"
exec unshare $UNSHARE_OPTS setarch $ARCH_OPTS ~/gdb-13/gdb-13.2/gdb/gdb --args env LD_PRELOAD=./deps/dlmalloc/newdl.so G_SLICE=always-malloc "$@"
#setarch $ARCH_OPTS unshare $UNSHARE_OPTS "$@"
