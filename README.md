## Repository Structure

The Serverless platform main repository includes the components listed below. The documentation for each component
includes further information for the component.

- [Benchmarks](https://github.com/jovanstevanovic/serverless-architecture/blob/main/benchmarks/README.md) - contains
  a list of function examples that we are using as a load for testing.
- [Lambda Manager](https://github.com/jovanstevanovic/serverless-architecture/blob/main/lambda-manager/README.md) -
  written in Java using [Micronaut](https://guides.micronaut.io/index.html), contains core components of the system.
- [Tools](https://github.com/jovanstevanovic/serverless-architecture/blob/main/tools/README.md) - written in
  Python, contain tools for testing and plotting.

---

## Setup

For project setup, we need to do the following things:

1. Clone the project into a local repository.
2. Contact any contributor on this project to get access to a directory with all **resources**. Move **resources**
   folder to **lambda manager** directory.
3. Export path to **run** command-line tool with `export PATH=path/to/serverless-project/tools/bin:$PATH`.

---

## Testing

For testing purposes, we need two terminals (**T1** and **T2**):

1. (T1) Go to lambda manager's directory with `cd lambda-manager`. Build the lambda manager with Gradle using the
   command
   `./gradlew clean assemble`.
2. (T1) Run lambda manager with command `sudo java -jar build/libs/lambda-manager-1.0-all.jar`.
3. (T2) Go to the directory with benchmark which you want to use as load for testing. For example,
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

## Get Support

- Open a [GitHub issue](https://github.com/jovanstevanovic/serverless-architecture/issues) for bug reports, questions,
  or requests for enhancements.
  
