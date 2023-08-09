#!/bin/bash

if [ -z "$JAVA_HOME" ]
then
    echo "Please set JAVA_HOME first. It you don't have GraalVM locally, download, extract, and set JAVA_HOME."
    echo "> wget https://download.oracle.com/graalvm/17/latest/graalvm-jdk-17_linux-x64_bin.tar.gz"
    echo "> tar -vzxf graalvm-jdk-17_linux-x64_bin.tar.gz"
    echo "> export JAVA_HOME=$(pwd)/graalvm-jdk-17.0.7+8.1"
    exit 1
fi

# Install GraalVM components.
$JAVA_HOME/bin/gu install native-image python nodejs

# Install graalpy packages in virtual env.
VENV=$JAVA_HOME/graalvisor-python-venv
$JAVA_HOME/bin/graalpy -m venv $VENV
source $VENV/bin/activate
graalpy -m ginstall list
graalpy -m ginstall install numpy
graalpy -m ginstall install Pillow
graalpy -m ginstall install requests
graalpy -m ginstall install torch
graalpy -m ginstall install torchvision
graalpy -m ginstall install texttable
graalpy -m ginstall install igraph
