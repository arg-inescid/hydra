#!/bin/bash

function copy_deps {
    # Use ldd to look for dependencies.
    for dep in $(ldd $1 | grep "=" | grep -v "not found" | awk '{ print $3 }')
    do
        if [ ! -f "$DISK/$dep" ]; then
            echo "Copying $dep as a dependency of $1"
            mkdir -p $DISK/$(dirname $dep)
            cp $dep $DISK/$(dirname $dep)
            # Dependencies might have dependencies as well.
            copy_deps $dep
        fi
    done
}
