#!/bin/bash

function copy_polyglot_deps {
    mkdir -p $DISK/jvm/languages

    read -p "Include Python dependencies in image (y or Y, everything else as no)? " -n 1 -r
    echo    # move to a new line
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        # These dependencies might be necessary for pytorch.
        # It depents on the system that you installed the graalpy python packages.
        # Ideally we would install them on a container.
        mkdir -p $DISK/usr/lib/x86_64-linux-gnu/
        cp /usr/lib/x86_64-linux-gnu/libmpi_cxx.so.20.10.0 $DISK/usr/lib/x86_64-linux-gnu/
        cp /usr/lib/x86_64-linux-gnu/libmpi.so.20          $DISK/usr/lib/x86_64-linux-gnu/
        cp /usr/lib/x86_64-linux-gnu/libopen-rte.so.20     $DISK/usr/lib/x86_64-linux-gnu/
        cp /usr/lib/x86_64-linux-gnu/libopen-pal.so.20     $DISK/usr/lib/x86_64-linux-gnu/
        cp /usr/lib/x86_64-linux-gnu/libhwloc.so.5         $DISK/usr/lib/x86_64-linux-gnu/
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
