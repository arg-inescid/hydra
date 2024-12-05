#!/bin/bash

# This is a smoke-test for Lambda Manager and GV/OW runtimes for different languages.
# It uploads functions with different runtimes and languages, and performs a single invocation to every registered function.
# The functions in this script are the typical functions from the benchmark suite we use for evaluation.
# NOTE: this script requires the "web" container to be started (see benchmarks/data/start-webserver.sh).

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

FUNCTION_MEMORY=1024

start_lambda_manager $(DIR)/config.json $(DIR)/variables.json
sleep 5


# Upload Java functions.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvhw\&function_language=java\&function_entry_point=com.hello_world.HelloWorld\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=isolate -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-jv-hello-world.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvhwow\&function_language=java\&function_entry_point=Main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/java/cr-hello-world/init.json"

curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvfh\&function_language=java\&function_entry_point=com.filehashing.FileHashing\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=isolate -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-jv-file-hashing.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvfhow\&function_language=java\&function_entry_point=Main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/java/cr-file-hashing/init.json"

curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvhr\&function_language=java\&function_entry_point=com.httprequest.HttpRequest\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=isolate -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-jv-httprequest.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jvhrow\&function_language=java\&function_entry_point=Main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/java/cr-httprequest/init.json"

# Upload JavaScript functions.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jshw\&function_language=java\&function_entry_point=com.helloworld.HelloWorld\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=context-snapshot\&svm_id=1 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-js-hello-world.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jshwow\&function_language=javascript\&function_entry_point=main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/javascript/cr-hello-world/init.json"

curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jsdh\&function_language=java\&function_entry_point=com.dynamichtml.DynamicHTML\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=context-snapshot\&svm_id=3 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-js-dynamic-html.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jsdhow\&function_language=javascript\&function_entry_point=main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/javascript/cr-dynamic-html/init.json"

curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jsup\&function_language=java\&function_entry_point=com.uploader.Uploader\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=context-snapshot\&svm_id=2 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-js-uploader.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=jsupow\&function_language=javascript\&function_entry_point=main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/javascript/cr-uploader/init.json"

# Upload Python functions.
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=pyhw\&function_language=java\&function_entry_point=com.helloworld.HelloWorld\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=context-snapshot\&svm_id=4 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-py-hello-world.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=pyhwow\&function_language=python\&function_entry_point=main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/python/cr-hello-world/init.json"

curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=pyup\&function_language=java\&function_entry_point=com.uploader.Uploader\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=context-snapshot\&svm_id=5 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-py-uploader.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=pyupow\&function_language=python\&function_entry_point=main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/python/cr-uploader/init.json"

curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=pyco\&function_language=java\&function_entry_point=com.compression.Compression\&function_memory=$FUNCTION_MEMORY\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true\&gv_sandbox=context-snapshot\&svm_id=6 -H 'Content-Type: text/plain' --data "http://172.18.0.1:8000/apps/gv-py-compression.so"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=user\&function_name=pycoow\&function_language=python\&function_entry_point=main\&function_memory=$FUNCTION_MEMORY\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: text/plain' --data "$ARGO_HOME/benchmarks/src/python/cr-compression/init.json"


# Make requests to Java functions.
echo -e "${GREEN}Invoking jvhw...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jvhw -H 'Content-Type: application/json' --data '{ }'
echo -e "${GREEN}Invoking jvhwow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jvhwow -H 'Content-Type: application/json' --data '{ }'

echo -e "${GREEN}Invoking jvfh...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jvfh -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
echo -e "${GREEN}Invoking jvfhow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jvfhow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

echo -e "${GREEN}Invoking jvhr...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jvhr -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
echo -e "${GREEN}Invoking jvhrow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jvhrow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

# Make requests to JavaScript functions.
echo -e "${GREEN}Invoking jshw...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jshw -H 'Content-Type: application/json' --data '{ }'
echo -e "${GREEN}Invoking jshwow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jshwow -H 'Content-Type: application/json' --data '{ }'

echo -e "${GREEN}Invoking jsdh...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jsdh -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/template.html","username":"user","nsize":"10"}'
echo -e "${GREEN}Invoking jsdhow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jsdhow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/template.html","username":"user","nsize":"10"}'

echo -e "${GREEN}Invoking jsup...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jsup -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
echo -e "${GREEN}Invoking jsupow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/jsupow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

# Make requests to Python functions.
echo -e "${GREEN}Invoking pyhw...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/pyhw -H 'Content-Type: application/json' --data '{ }'
echo -e "${GREEN}Invoking pyhwow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/pyhwow -H 'Content-Type: application/json' --data '{ }'

echo -e "${GREEN}Invoking pyup...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/pyup -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
echo -e "${GREEN}Invoking pyupow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/pyupow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

echo -e "${GREEN}Invoking pyco...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/pyco -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/video.mp4"}'
echo -e "${GREEN}Invoking pycoow...${NC}"
curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/user/pycoow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/video.mp4"}'


stop_lambda_manager
