## Repository Structure
The Serverless platform main repository includes the components listed below. The documentation for each component 
includes further information for the component.
  - [Benchmarks](https://github.com/jovanstevanovic/serverless-architecture/blob/main/lambda-manager/README.md) -
  contains a list of function examples that we are using as a load for testing.
  - [Lambda Manager](https://github.com/jovanstevanovic/serverless-architecture/blob/main/lambda-manager/README.md) -
  written in Java using [Micronaut](https://guides.micronaut.io/index.html), contains core components of the system.
  - [Tools](https://github.com/jovanstevanovic/serverless-architecture/blob/main/lambda-manager/README.md) -
  written in Python, contain tools for testing and plotting.

---
## Setup
For project setup, we need to do the following things:
  1. Clone the project into a local repository.
  2. Contact any contributor on this project to get access to a directory with all **resources**. Move **resource**
     folder to **lambda manager** directory.
  3. Run the command `sudo apt-get install -y python3` to install python.
  4. Run the command `sudo python3 tools/install-deps.py` to installing all necessary dependencies.

---
## Testing
For testing purposes, we need two terminals (**T1** and **T2**):

  1. (T1) Build the project with Gradle using the command `./gradlew clean assemble`.
  2. (T1) Run lambda manager with command `sudo java -jar build/libs/lambda-manager-1.0-all.jar`.
  3. (T2) Go to the tool's directory with the command `cd tools`.
  4. (T2) Change manager's configuration `default-configs/manager.json` or leave it with default values.
  5. (T2) Change test's configuration `default-configs/test.json` or leave it with default values.
  6. (T2) Start testing tool with command `python3 run-test.py default-configs/test.json`.

---
## Plotting
For plotting purposes, we will need results from the testing phase and plotting tool.
  1. Go to tools directory with command `cd tools`.
  2. Change the plot's configuration `default-configs/test.json` or leave it with default values.
  3. Start plotting tool with command `python3 run-plot.py default-configs/plot.json`.

---
## Get Support
- Open a [GitHub issue](https://github.com/jovanstevanovic/serverless-architecture/issues) for bug reports, questions, 
  or requests for enhancements.
  
