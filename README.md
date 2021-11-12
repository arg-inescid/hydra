## Lambda Manager
The Lambda Manager is the core component of the GraalServerless (a.k.a. Argo) project. The project vision and initial experimental results are accessible in [Argo's README](doc/Argo.md) page.

The project includes a number of repositories:
- [benchmarks](https://github.com/graalvm-argo/benchmarks/blob/main/README.md) - Contains multiple benchmarks in multiple languages that are used to benchmark the project;
- [lambda-proxies](https://github.com/graalvm-argo/lambda-proxies/blob/main/README.md) - Contains Serverless function wrappers that offer support for Native Image Isolates and Truffle Languages;
- [lambda-manager](https://github.com/graalvm-argo/lambda-manager/blob/main/README.md) - (this repository) The Lambda Manager is a core component of the Argo architecture. It manages the resources of a local node by launching and terminating VMs where functions run;
- [cluster-manager](https://github.com/graalvm-argo/cluster-manager/blob/main/README.md) - The Cluster Manager is the component that overseas a number of Lambda Managers and decides where function invocations should be sent based on resource utilization (**prototype only**);
- [load-balancer](https://github.com/graalvm-argo/load-balancer/blob/main/README.md) - An NGINX-based network load balancer that balances load for a number of Cluster Managers (**prototype only**);
- [web-ui](https://github.com/graalvm-argo/web-ui/blob/master/README.md) - WebUI gives opportunities to users to upload functions, invoke functions, and see results in real time;
- [run](https://github.com/graalvm-argo/run/blob/main/README.md) - Run is a command-line tool for testing, plotting, running gates, updating project, and installing dependencies.

---

## Setup

To set up project, we need to do the following things:

1. Clone all repositories.
2. Contact any contributor on this project to get access to a directory with all **resources**. Place **resources**
   folder in project root.
3. Export path to **run** command-line tool with `export PATH=path/to/argo-root/run/bin:$PATH`.
4. Add `source path/to/argo-root/run/bin/run_completion.sh` in your `~/.bashrc` or `~/.bash_profile` script if you
   want to enable auto-completion.
5. Run `run install-deps` for installing all dependencies.

---

## Testing

For testing purposes, we need two terminals (**T1** and **T2**):

1. (T1) Go to lambda manager's directory with `cd lambda-manager`.
2. (T1) Build and run lambda manager with command `run manager`.
3. (T2) Go to the directory with benchmark that you want to use as load for testing. For example,
   `cd benchmarks/language/java/hello-world`.
4. (T2) Build benchmark with command `./gradlew clean assemble`. Go back to project's root directory with `cd ../..`
5. (T2) Go to the tool's directory with the command `cd tools`.
6. (T2) Change manager's configuration `configs/manager/local-manager.json` or leave it with default values.
7. (T2) Change test's configuration `configs/tests/tier-1/hello-world.json` or leave it with default values.
8. (T2) Start testing lambda manager with command `run test configs/tests/tier-1/hello-world.json.`

---

## Plotting

For plotting purposes, we will need results from the testing phase and plotting tool.

1. Go to tools directory with command `cd tools`.
2. Change the plot's configuration `configs/plot/plot-all.json` or leave it with default values.
3. Start plotting tool with command `run plot configs/plot/plot-all.json`.

---

## Lambda Manager Structure

The Lamabda Manager is written in Java using [Micronaut](https://guides.micronaut.io/index.html). Its main components are the following:

- `Encoder` - Class for transforming username and lambda name to unique name, which is then used as the key for Function
  Storage.
- `Function Storage` - Class for storing meta-information about every registered function. Like ID, function name,
  available instances, created instances, active instances, opened HTTP connections...
- `Code Writer` - Class for storing binary code of servers writing them on the same disk as lambda manager.
- `Scheduler` - Class which is deciding which instance of lambda should we call.
- `Optimizer` - Class which is deciding whether to start a new instance of a lambda with as **Hotspot** or **VMM**.
  Server as next step in execution pipe (after `Encoder ` and `Scheduler` and before `Client`).
- `Client` - Class for making connections toward lambdas.
- `Lambda Manager` - Core class which is just a template while all implementations are kept in concrete implementations
  of Interfaces for all above classes.
