# Lambda Manager Tests

This directory contains files and directories that can be used to test Lambda Manager (LM). These are primarily smoke tests that allow developers to make sure their changes did not break the most essential parts of LM's functionality. However, more specific test cases can be added to test some particular feature in LM.

## Running Tests

Normally, each test directory contains 3 files:

* `test.sh` - the actual test script. Run this script to execute the test;
* `config.json` - the main LM configuration;
* `variables.json` - the global variables configuration for LM. Usually, all values there are the same for all tests.

In order to run some test, just run the `test.sh` script in the test's directory.

**Note:** if you intend to use functions that make an HTTP request (like Uploader, Compression, FileHashing, etc.), make sure to start the "web" container (see `benchmarks/data/start-webserver.sh`).

**Note:** the tests usually use the HTTP-style communication with LM. If you want to test the socket server functionality, read the section below.

## Testing Socket Server

If you want to test functionality specific to the LM's socket server, you can run the `test.sh` script inside the `socket-server` directory. It will not run any tests automatically; instead, the script will launch the client waiting for your input. Feel free to change `config.json` or `variables.json` in the `socket-server` directory if needed.

This client composes messages according to the format expected by the LM's socket server. Using this client, you can upload new functions to LM and invoke them.

The format for the upload/invoke commands is as follows:

```
# "u" stands for "upload"
u username=... function_name=... function_language=... function_entry_point=... function_memory=... function_runtime=... function_isolation=... invocation_collocation=... [gv_sandbox=...] [svm_id=...] '/path/to/your/function'

# "i" stands for "invoke"
i username=... function_name=... '{...}'
```

The socket client accepts the same parameters as the HTTP controller with its query parameters. Unlike in HTTP, where query parameters are separated by the ampersand sign (&), in the socket client, all parameters are separated with spaces. Payload (path to the function code or invocation arguments) can be provided using single quotes ('). **Note:** it is important that the payload is quoted with single quotes, as LM searches for single quotes to identify the payload.

### Example commands for socket server

**Note:** make sure to update the function code paths.

Upload and run Java HelloWorld in Hydra:

```
> u username=user function_name=jvhw function_language=java function_entry_point=com.hello_world.HelloWorld function_memory=256 function_runtime=graalvisor function_isolation=false invocation_collocation=true gv_sandbox=isolate '/full/path/to/benchmarks/src/java/gv-hello-world/build/libhelloworld.so'

> i username=user function_name=jvhw '{}'
```

Upload and run Java Sleep in Hydra:

```
> u username=user function_name=jvsl function_language=java function_entry_point=com.sleep.Sleep function_memory=256 function_runtime=graalvisor function_isolation=false invocation_collocation=true gv_sandbox=isolate '/full/path/to/benchmarks/src/java/gv-sleep/build/libsleep.so'

> i username=user function_name=jvsl '{"sleep":"9000","memory":"128"}'
```

Upload and run Python Compression in OpenWhisk (note the function size; update `maxMemory` in `config.json` accordingly):

```
> u username=user function_name=pyhw function_language=python function_entry_point=main function_memory=2048 function_runtime=openwhisk function_isolation=true invocation_collocation=false '/full/path/to/benchmarks/src/python/cr-compression/init.json'

> i username=user function_name=pyhw '{"url":"http://172.18.0.1:8000/snap.png"}'
```

Upload and run Java HelloWorld in GraalOS:

```
> u username=user function_name=graalos_test function_language=java function_memory=256 function_runtime=graalos function_isolation=true invocation_collocation=false '/path/to/graalos/benchmarks/graalos-client/apps/simple-http/build/native/nativeCompile/simple-http'

> i username=user function_name=graalos_test '{}'
```

You can also use the HTTP requests below to test GraalOS manually:

```
# Upload the function
> curl -s -X POST localhost:30008/upload_function?username=user\&function_name=graalos_test\&function_language=java\&function_entry_point=null\&function_memory=1024\&function_runtime=graalos\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../graalos/benchmarks/graalos-client/apps/simple-http/build/native/nativeCompile/simple-http"

# Invoke the function
> curl -s -X POST localhost:30008/user/graalos_test -H 'Content-Type: application/json' --data '{ }'
```