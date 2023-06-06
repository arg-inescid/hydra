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

    # TODO - eventually we also need to get rid of this. Benchmarks should include their own deps.
    if [ -z "$BENCHMARKS_HOME" ]
    then
        echo "Warning: BENCHMARKS_HOME is not set. Some benchmarks might now work due to missing dependencies."
    else
        # JVips.jar
        unzip -o -q $BENCHMARKS_HOME/src/javascript/gv-thumbnail/jvips/JVips.jar -d /tmp/jvips
        for dep in /tmp/jvips/*.so; do copy_deps $dep; done
    fi

    mkdir -p $DISK/jvm/languages

    read -p "Include Python dependencies in image (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        # Graalpython's Pillow package.
        copy_deps ~/.cache/Python-Eggs/Pillow-6.2.0-py3.8-linux-x86_64.egg-tmp/PIL/_imaging.graalpython-38-native-x86_64-linux.so
        # Copy graalvm python language libs and python's virtual environment.
        cp -r $JAVA_HOME/languages/{python,llvm} $DISK/jvm/languages
        cp -r $JAVA_HOME/graalvisor-python-venv $DISK/jvm
    fi

    read -p "Include JavaScript dependencies in image (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        # Copy graalvm js language libs
        cp -r $JAVA_HOME/languages/js $DISK/jvm/languages
    fi
}
