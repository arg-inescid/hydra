## Lambda Manager
The Lambda Manager is the core component of the GraalServerless (a.k.a. Argo) project.

---

## Lambda Manager API

The Lamabda Manager API is implemented in the [Controller](src/main/java/com/lambda_manager/LambdaManagerController.java).

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

- [`Coder`](src/main/java/com/lambda_manager/encoders/Coder.java) - Class for transforming username and function name to unique name, which is then used as the key for Function
  Storage;
- [`In-Memory Function Cache`](src/main/java/com/lambda_manager/function_storage/FunctionStorage.java) - Stores meta-information about every registered function. Like ID, function name, available instances, created instances, active instances, opened HTTP connections;
- [`Scheduler`](src/main/java/com/lambda_manager/schedulers/Scheduler.java) - Decides in which VM a particular function invocation should take place;
- [`HTTP Client`](src/main/java/com/lambda_manager/client/LambdaManagerClient.java) - Prepares TCP connections to VMs executing function invocations;
- [`Lambda Manager`](src/main/java/com/lambda_manager/core/LambdaManager.java) - Core class that handles function requests.
