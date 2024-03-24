#!/bin/bash

if [ -z "${ARGO_HOME}" ]; then
    echo "ARGO_HOME is not defined. Existing..."
    exit 1
fi

if [ -z "${JAVA_HOME}" ]; then
    echo "JAVA_HOME is not defined. Existing..."
    exit 1
fi

if ! grep -q GRAALVM_VERSION $JAVA_HOME/release; then
    echo "Error: JAVA_HOME does not point to a GraalVM distrubution: $JAVA_HOME"
    exit 1
else
    eval $(echo "export $(cat $JAVA_HOME/release | grep JAVA_VERSION=)")
    eval $(echo "export $(cat $JAVA_HOME/release | grep GRAALVM_VERSION=)")
fi

echo "ARGO_HOME = $ARGO_HOME"
echo "JAVA_HOME = $JAVA_HOME"
echo "JAVA_VERSION = $JAVA_VERSION"
echo "GRAALVM_VERSION = $GRAALVM_VERSION"

if ! command -v firecracker &> /dev/null
then
    echo "WARNING: firecracker could not be found!"
    echo "Firecracker should be downloaded and added to the path. For example:"
    echo "> wget https://github.com/firecracker-microvm/firecracker/releases/download/v1.1.0/firecracker-v1.1.0-x86_64.tgz"
    echo "> tar -vzxf firecracker-v1.1.0-x86_64.tgz"
    echo "> mv release-v1.1.0-x86_64 /opt/firecracker-v1.1.0-x86_64"
    echo "> ln -s /opt/firecracker-v1.1.0-x86_64/firecracker-v1.1.0-x86_64 /opt/firecracker-v1.1.0-x86_64/firecracker"
    echo "> export PATH=\$PATH:/opt/firecracker-v1.1.0-x86_64"
    echo "> rm firecracker-v1.1.0-x86_64.tgz"
fi

if ! command -v docker &> /dev/null
then
    echo "WARNING: docker could not be found!"
    GRAALVISOR_BUILD_MODE="local"
fi

if [ ! -f $ARGO_HOME/resources/hello-vmlinux.bin ];
then
    echo "Downloading linux kernel image..."
    curl -fsSL -o $ARGO_HOME/resources/hello-vmlinux.bin https://s3.amazonaws.com/spec.ccfc.min/img/hello/kernel/hello-vmlinux.bin
    echo "Downloading linux kernel image... done!"
fi

read -p "Build lambda manager? (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $ARGO_HOME/lambda-manager/build.sh
fi

read -p "Build builder container image? (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $ARGO_HOME/builder/build.sh
fi

read -p "Build graalvisor? (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $ARGO_HOME/graalvisor/build.sh $GRAALVISOR_BUILD_MODE
fi

bash $ARGO_HOME/images/build.sh
bash $ARGO_HOME/benchmarks/scripts/build_benchmarks.sh
