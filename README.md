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
- Set your ARGO_HOME to the root of the Hydra repository.

If you want to run a single instance of the Hydra runtime independently from the platform, please refer to the README instructions in the [`hydra`](hydra) directory.

The Hydra platform can be easily launched locally for test and development purposes using the instructions in the [lambda-manager's](lambda-manager/README.md) page.

For a full deployment with a top-level scheduler, please refer to the README instructions of the [top-level scheduler](https://github.com/arg-inescid/hydra-scheduler).

### Contacts

Contact Serhii Ivanenko (serhii.ivanenko@tecnico.ulisboa.pt) should you have any questions regarding the Hydra runtime or Hydra platform.

### Acknowledgements

This work was supported by a grant from Oracle Labs and by national funds through Fundação para a Ciência e a Tecnologia (FCT) under projects UID/50021/2025, UID/PRR/50021/2025, LISBOA2030-FEDER-00748300, and FCT scholarship 2024.01902.BD.
