# Hydra Runtime

This directory contains the Hydra runtime's codebase.

### Building the runtime

**IMPORTANT**: make sure to go to the [`builder`](../builder) directory and run the `build.sh` script there to obtain a builder Docker image. It will be used to build the Hydra runtime.

Run the `build.sh` script to build the Hydra runtime binary. After running this script, you can use the [`hydra`](./hydra) script to launch Hydra runtime locally.

If you want to run Hydra in a containerized environment, go to [`images`](../images) and run the `build.sh` script - answer "y" when it prompts about "Hydra container". After this, you can run Hydra in a container. **IMPORTANT**: Hydra requires the `--privileged` flag if running an instance as a Docker container due to system call tracking for sandbox Checkpoint/Restore mechanism.

### Usage

The easiest way to deploy and test a single Hydra runtime instance is to run it with a [benchmark-hydra.sh](../benchmarks/scripts/benchmark-hydra.sh) script - run it without any arguments to get help. Note that you need to build the benchmarks you want to use beforehand.

You can also run the Hydra runtime yourself without the script - consult the [shared.sh](../benchmarks/scripts/shared.sh) script to see the commands. Once started up, Hydra runtime exposes HTTP endpoints to register and invoke functions. The code of the register/invoke handlers is available in [`RuntimeProxy.java`](src/main/java/org/graalvm/argo/hydra/RuntimeProxy.java).

Register endpoint only accepts HTTP query parameters (i.e., no body). The main parameters for the register endpoint are:

- `name` - name of the function;
- `url` - URL to the function code, usually an *.so or a *.zip. Hydra expects function code to be hosted on some web server. See the last step of the **Building all components** section of the main [`README.md`](../README.md) for more details;
- `entryPoint` - Java entrypoint (fully-qualified class name, including the package name);
- `language` - language of the function (suppored languages are `java`, `javascript`, and `python`);
- `sandbox` - sandboxing type to use for the function. Defaults to `isolate`. The other supported sandboxes are `process`, `snapshot` (for sandbox C/R), and `snapshot-process` (also for sandbox C/R, but restore takes place in a subprocess);
- `svmid` - numeric value that needs to be unique for each function and needs to be the same for checkpoint and restore process. Only to be used with `snapshot` and `snapshot-process` sandboxing types.

Register endpoint only accepts a JSON body (i.e., only the body, without query parameters). The main JSON keys for the invoke endpoint are:

- `name` - JSON string with the name of the function;
- `arguments` - JSON string with the arguments passed to the function, commonly another JSON.
