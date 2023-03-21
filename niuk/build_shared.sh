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

function copy_polyglot_deps {
    mkdir -p $DISK/jvm/languages

    # Copy necessary truffle language libraries.
    if [[ $LANGS == *"--language:python"* ]]; then
        # Graalpython's Pillow package.
        copy_deps ~/.cache/Python-Eggs/Pillow-6.2.0-py3.8-linux-x86_64.egg-tmp/PIL/_imaging.graalpython-38-native-x86_64-linux.so
        # Copy graalvm python language libs and python's virtual environment.
        cp -r $ghome/languages/{python,llvm} $DISK/jvm/languages
        cp -r $ghome/graalvisor-python-venv $DISK/jvm
    fi

    if [[ $LANGS == *"--language:js"* ]]; then
        # Copy graalvm js language libs
        cp -r $ghome/languages/js $DISK/jvm/languages
    fi
}
