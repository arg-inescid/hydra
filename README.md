# Hydra

Hydra is a serverless solution aimed at maximizing function invocation density. The solution consists of two main components:

- Hydra runtime, a virtualized multi-language runtime capable of colocating multiple invocations of different functions;
- Hydra platform featuring a colocation-aware scheduler and a node manager maintaining a pool of pre-warmed Hydra runtime instances.

Hydra runtime relies on GraalVM Native Image Isolates for lightweight sandboxing and on Truffle for multi-language support. The runtime also features a Checkpoint/Restore (C/R) mechanism for individual sandboxes.

The Hydra platform features scheduling policies for different colocation strategies. The platform consists of two components: the [top-level scheduler](https://github.com/arg-inescid/hydra-scheduler) that can act as a benchmarking tool, and a local [node manager](lambda-manager) to manage a set of Hydra instances. The node manager also supports other runtimes, such as OpenWhisk and Knative, and can be deployed independently from the top-level scheduler. The top-level scheduler potentially manages multiple nodes (each running a node manager), and each node manager manages multiple Hydra runtime instances.

### Paper

The [SoCC'25 paper](https://doi.org/10.1145/3772052.3772267) presents Hydra as a co-design of the runtime and the platform to maximize density, eliminate cold starts, and reduce memory utilization in serverless deployments. The paper details the design and implementation of various aspects of the runtime and the platform.

### Supported platforms
The project is under development, and it's tested on Ubuntu 22.04.4 LTS and Debian 10.

### Repository structure

This repository depends on two other repositories in the form of git submodules:

- [`benchmarks`](https://github.com/arg-inescid/hydra-benchmarks) - a repository containing source code and build scripts of the benchmarks used for evaluating Hydra's performance;
- [`scheduler`](https://github.com/arg-inescid/hydra-scheduler) - the aforementioned top-level scheduler.

Inside this repository, you will find the following main directories:

- [`builder`](builder) - contains files to produce a Docker image used to build the Hydra runtime;
- [`hydra-lib`](hydra-lib) - contains an API shared between the Hydra runtime and the benchmarks;
- [`hydra`](hydra) - the Hydra runtime codebase;
- [`images`](images) - contains scripts and auxiliary files to generate Docker images of the runtimes to be used in the platform;
- [`lambda-manager`](lambda-manager) - the node manager codebase (part of the platform).

Please refer to the README files of the corresponding repositories or directories to get more details.

### Build and deploy

Prerequisites:

- Set your JAVA_HOME to a GraalVM distribution of JVM. Tested with GraalVM Community 23.0.0 for Java 17.0.7;
- Set your ARGO_HOME to the root of the Hydra repository (this directory).

Each directory contains a README.md file with instructions how to build and/or deploy the corresponding component with additional notes and details.

Below, you can find quick instructions on how to get every component built and ready for deployment.

#### Building and preparing all components

1. Go to [`builder`](builder) and run `build.sh` to build the Docker image used when building the Hydra runtime;
2. Go to [`hydra`](hydra) and run `build.sh` to build the Hydra runtime. After this step, you can run the Hydra runtime independently;
3. Go to [`images`](images) and run `build.sh` - enter 'y' when prompted to build the Hydra container. After this step, you can run the Hydra runtime in a container independently;
4. Go to [`lambda-manager`](lambda-manager) and run `build.sh`. After this step, you can run a local node manager;
5. Go to [`benchmarks/src/<language>/<benchmark>`](benchmarks/src) and run `build_script.sh` of the chosen benchmark(s). It will build the binary (`.so`) of the chosen benchmark(s);
6. Go to [`benchmarks/scripts`](benchmarks/scripts) and run `install_benchmarks.sh` script. Feel free to comment out the benchmarks you didn't build in the script. This script copies the benchmark binaries to [`benchmarks/data/apps`](benchmarks/data/apps) to later be served by the web server (see next step);
7. Go to [`benchmarks/data`](benchmarks/data) and run `start-webserver.sh` to start up the Docker container running an NGINX server hosting the files used by some benchmarks. It also serves the benchmark binaries (to be pulled by the Hydra runtime during registration).

#### Running Hydra

If you want to run a single instance of the Hydra runtime independently from the platform, please refer to the README instructions in the [`hydra`](hydra) directory.

The Hydra platform can be easily launched locally for test and development purposes using the instructions in the [lambda-manager's](lambda-manager/README.md) page.

For a full deployment with a top-level scheduler, please refer to the README instructions of the [top-level scheduler](https://github.com/arg-inescid/hydra-scheduler).

### Troubleshooting

Some common known issues when using the Hydra runtime and/or the Hydra platform are listed below. We are working on long-term solutions addressing these issues.

#### 1. "Permission denied" when registering the function.

This problem can happen when you run a Docker container with Hydra runtime and share the `apps` directory with your host. By default, the process inside the container runs as `root`, whereas the directory on your host is likely to be owned by your user. This mismatch can lead to the following logs of the Hydra process:

```
Downloading http://X.X.X.X:8000/apps/hy-py-hello-world.so
java.io.FileNotFoundException: /tmp/apps/hy-py-hello-world.so (Permission denied)
        at java.base@17.0.7/java.io.FileOutputStream.open0(Native Method)
        ...
        at org.graalvm.argo.hydra.RuntimeProxy$RegisterHandler.handleInternal(RuntimeProxy.java:358)
        ...
```

In particular, this can happen if you build everything, install some benchmark, and try to run it with `benchmark-hydra.sh` as follows:

```
$ export WORK_DIR=/tmp/workdir
$ bash benchmark-hydra.sh container hy_java_hw test 1
```

**Temporary workaround:** run this script with `sudo` as follows:

```
$ sudo WORK_DIR=/tmp/workdir bash benchmark-hydra.sh container hy_java_hw test 1
```

#### 2. Hydra runtime hangs when checkpointing/restoring the function.

The root cause of the problem is similar to the previous one - sharing the `apps` directory and the mismatch of the users running the process inside the container and owning the `apps` directory on host. You can observe such a behavior when running the [`prepare-snapshots`](lambda-manager/tests/prepare-snapshots) test that generates function snapshots - you will see the following logs being printed repeatedly in an `error.log` file of the corresponding lambda (Hydra runtime instance):

```
error: failed to serialize memory tag: Bad file descriptor
error: failed to serialize memory header: Bad file descriptor
error: failed to serialize mem_allocator tag: Bad file descriptor
...
error: failed to serialize thread context: Bad file descriptor
error: failed to serialize clone args: Bad file descriptor
...
error: failed to read tagerror: unknown tag during get_syscall_size: 32767organize_syscall: data size is larger than buf size (500 bytes).
...
```

**Temporary workaround:** change the owner of your [`benchmarks/data/apps`](benchmarks/data/apps) directory (where you have your benchmark binaries installed) to `root:root`.

### Contacts

Contact Serhii Ivanenko (serhii.ivanenko@tecnico.ulisboa.pt) should you have any questions regarding the Hydra runtime or Hydra platform.

### Acknowledgements

This work was supported by a grant from Oracle Labs and by national funds through Fundação para a Ciência e a Tecnologia (FCT) under projects UID/50021/2025, UID/PRR/50021/2025, LISBOA2030-FEDER-00748300, and FCT scholarship 2024.01902.BD.
