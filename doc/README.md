## Argo Structure

The Argo main repository includes the components listed below. The documentation for each component includes further
information for the component.

- [benchmarks](https://github.com/argo-com/benchmarks/blob/main/README.md) - List of all benchmarks used for system
  testing.
- [lambda-proxies](https://github.com/argo-com/lambda-proxies/blob/main/README.md) - List of all proxies that the system
  is using during lambda invocation.
- [lambda-manager](https://github.com/argo-com/lambda-manager/blob/main/README.md) - The Lambda manager is a core
  component of the Argo architecture.
- [argo-ui](https://github.com/argo-com/argo-ui/blob/master/README.md) - ArgoUI gives opportunities to users to upload
  functions, measure times, and plot functions.
- [run](https://github.com/argo-com/run/blob/main/README.md) - Run is a command-line tool for testing, plotting, running
  gates, updating project, installing dependencies...

---

## Setup

To set up project, we need to do the following things:

1. Clone all repositories.
2. Contact any contributor on this project to get access to a directory with all **resources**. Place **resources**
   folder in project root.
3. Export path to **run** command-line tool with `export PATH=path/to/argo-root/tools/bin:$PATH`.
4. Add `source path/to/argo-root/tools/bin/run_completion.sh` in your `~/.bashrc` or `~/.bash_profile` script if you
   want to enable auto-completion.

---

## Testing

For testing purposes, we need two terminals (**T1** and **T2**):

1. (T1) Go to lambda manager's directory with `cd lambda-manager`. Build the lambda manager with Gradle using the
   command `./gradlew clean assemble`.
2. (T1) Run lambda manager with command `sudo java -jar build/libs/lambda-manager-1.0-all.jar`.
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

## ArgoUI and monitoring

The [ArgoUI](https://github.com/argo-com/argo-ui) makes it easier to deploy, test and monitor your functions. The
following diagram shows data flows between different parts of the system for monitoring purposes:

![data flow diagram](resources/data_flow.jpg)

---

## Get Support

- Open a [GitHub issue](https://github.com/argo-com/lambda-manager/issues) for bug reports, questions, or requests for
  enhancements.
  