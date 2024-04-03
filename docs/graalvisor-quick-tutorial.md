## Setting up Graalvisor

After you clone this repository, simply call the `setup.sh` script that resides in the top directory of the repository:

`bash setup.sh`

This script will ask you a number of questions which you can answer "no" or "n", in which case nothing will be done.

You will need a GraalVM distribution to compile Graalvisor so when asked to install GraalVM, you should answer "yes" or "y". 

Then, to compile Graalvisor you can do the following:

```
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
