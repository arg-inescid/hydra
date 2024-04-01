## Setting up Graalvisor

Assuming you have cloned this repository, you should create the following environment variable:

`export ARGO_HOME=<path to cloned repository>`

We will also need a GraalVM distribution, which can be easily set up with the following steps:

```
# Download tar.gz with GraalVM. This version has been tested to work with Graalvisor.
wget https://download.oracle.com/graalvm/17/archive/graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
# Untar it.
tar -vzxf graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
# Delete the tarbal, we don't need it anymore.
rm graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
# Move extracted JVM into $ARGO_HOME/resources
mv graalvm-jdk-17.0.7+8.1 $ARGO_HOME/resources
# Set up an environment variable that will be used later.
export JAVA_HOME=$ARGO_HOME/resources/graalvm-jdk-17.0.7+8.1
```

At this point, we can proceed with building Graalvisor. To do so, call the following script:

`$ARGO_HOME/setup.sh`

This script will ask you a number of questions. If you answer "no" or "n", nothing will be done. To compile Graalvisor you can do the following:

```
ARGO_HOME = /home/rbruno/git/graalserverless
JAVA_HOME = /home/rbruno/software/graalvm-jdk-17.0.7+8.1
JAVA_VERSION = 17.0.7
GRAALVM_VERSION = 23.0.0
Build lambda manager? (y or Y, everything else as no)? n
Build builder container image? (y or Y, everything else as no)? n
Build graalvisor? (y or Y, everything else as no)? y
Native JavaScript support (y or Y, everything else as no)? n
Native Python support (y or Y, everything else as no)? n
```

Note that we didn't include native JavaScript nor Python support. Those are not included by default. After a successful compilation you should see:

`Building graalvisor Native Image... done!`

The next step is to compile a simple Hello World application to test Graalvisor. That can be achieved by answering "y" or "yes" to

`Build graalvisor test (y or Y, everything else as no)?`

After the test is compiled you can execute it with the following two commands:

```
# This variable tells the benchmark scripts where to place temporary data and logs.
export WORK_DIR=/home/rbruno/git/graalserverless/tmp
# This script registers a Java Hello World function into Graalvisor and invokes it once.
sudo -E /home/rbruno/git/graalserverless/benchmarks/scripts/benchmark-graalvisor.sh svm gv_java_hw test 1
```

As a result you should see something like:
```
Running graalvisor environment=svm; sandbox=isolate; app=gv_java_hw; mode=test; workload=1; cpu=1; mem=2048:
Waiting for svm...
Waiting for svm... done (took 50285 us).
Function gv-hello-world registered!
Sending 1 requests:
Req latency 17606 us; Req output: {"result":"{"VM Context":"Substrate VM","Log":"Hello World"}","process time (us)":2145}
Saved logs (iteration 1): /home/rbruno/git/graalserverless/benchmarks/scripts/../results/benchmark/java/gv-hello-world-svm-cold-isolate-test-1-1-2048/1
```

That's it! You can call the `benchmark-graalvisor.sh` script with no arguments to see which other backends, sandboxes, and benchmarks are available.