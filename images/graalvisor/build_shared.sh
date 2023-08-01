#!/bin/bash

function copy_polyglot_deps {
    mkdir -p $DISK/jvm/languages

    read -p "Include Python dependencies in image (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
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
