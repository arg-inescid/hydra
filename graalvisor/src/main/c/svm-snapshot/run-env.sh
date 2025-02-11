#!/bin/sh

# You can add any default options here
UNSHARE_OPTS="unshare --map-root-user --keep-caps --mount-proc -f -p"
ARCH_OPTS="-R"

# Run the command with all arguments passed to the script
unshare $UNSHARE_OPTS setarch $ARCH_OPTS "$@"
