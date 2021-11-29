## Lambda Manager

The Lambda Manager is the core component of the GraalServerless (a.k.a. Argo) project.

---

## Lambda Manager API

The Lambda Manager API is implemented in the [Controller](src/main/java/org/graalvm/argo/lambda_manager/LambdaManagerController.java).

---

## Setup

To set up project, we need to do the following things:

1. Clone all repositories.
2. Contact any contributor on this project to get access to a directory with all **resources**. Place **resources**
   folder in project root.
3. Export path to **run** command-line tool with `export PATH=path/to/project-root/argo/run/bin:$PATH`.
4. Add `source path/to/project-root/argo/run/bin/run_completion.sh` in your `~/.bashrc` or `~/.bash_profile` script if
   you want to enable auto-completion.
5. Run `run install-deps` for installing all dependencies.

---

## Testing

For testing purposes, we need two terminals (**T1** and **T2**):

1. (T1) Build lambda manager with `run build lm`.
2. (T1) Deploy lambda manager with command `run deploy lm`.
3. (T2) Change manager's configuration `configs/manager/default-manager.json` or leave it with default values.
4. (T2) Change test's configuration `configs/tests/tier-1/java/hello-world.json` or leave it with default values.
5. (T2) Start testing lambda manager with command `run test configs/tests/tier-1/java/hello-world.json.`

---

## Plotting

For plotting purposes, we will need results from the testing phase and plotting tool.

1. Change the plot's configuration `configs/plot/plot-all.json` or leave it with default values.
2. Start plotting tool with command `run plot configs/plot/plot-all.json`.

---

## Lambda Manager Structure

The Lambda Manager is written in Java using [Micronaut](https://guides.micronaut.io/index.html). Its main components
are the following:

- [`Coder`](src/main/java/org/graalvm/argo/lambda_manager/encoders/Coder.java) - Class for transforming username and function name to
  unique name, which is then used as the key for Function Storage;
- [`In-Memory Function Cache`](src/main/java/org/graalvm/argo/cluster_manager/function_storage/FunctionStorage.java) - Stores
  meta-information about every registered function. Like ID, function name, available instances, created instances,
  active instances, opened HTTP connections;
- [`Scheduler`](src/main/java/org/graalvm/argo/cluster_manager/schedulers/Scheduler.java) - Decides in which VM a particular function
  invocation should take place;
- [`HTTP Client`](src/main/java/org/graalvm/argo/lambda_manager/client/LambdaManagerClient.java) - Prepares TCP connections to VMs
  executing function invocations;
- [`Lambda Manager`](src/main/java/org/graalvm/argo/lambda_manager/core/LambdaManager.java) - Core class that handles function
  requests.
