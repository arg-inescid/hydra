# This script is a smoke-test for Lambda Manager and GV/OW runtimes for different languages.
# It uploads configuration and functions with different runtimes and langauges, and performs a single invocation to every registered function.
# The functions in this script are the typical functions from the benchmark suite we use for evaluation.
# NOTE: this script requires the "web" container to be started (see benchmarks/data/start-webserver.sh).

# Upload configuration (make sure configuration has the lambda pool configured properly).
curl -s -X POST localhost:30009/configure_manager -H 'Content-Type: application/json' --data-binary @"$ARGO_HOME/run/configs/manager/default-lambda-manager.json"


# Upload Java functions.
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jvfh\&function_language=java\&function_entry_point=com.filehashing.FileHashing\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/java/gv-file-hashing/build/libfilehashing.so"
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jvfhow\&function_language=java\&function_entry_point=Main\&function_memory=256\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/java/cr-file-hashing/init.json"

curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jvhr\&function_language=java\&function_entry_point=com.httprequest.HttpRequest\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/java/gv-httprequest/build/libhttprequest.so"
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jvhrow\&function_language=java\&function_entry_point=Main\&function_memory=256\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/java/cr-httprequest/init.json"

# Upload JavaScript functions.
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jsdh\&function_language=java\&function_entry_point=com.dynamichtml.DynamicHTML\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/javascript/gv-dynamic-html/build/libdynamichtml.so"
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jsdhow\&function_language=javascript\&function_entry_point=main\&function_memory=256\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/javascript/cr-dynamic-html/init.json"

curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jsup\&function_language=java\&function_entry_point=com.uploader.Uploader\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/javascript/gv-uploader/build/libuploader.so"
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=jsupow\&function_language=javascript\&function_entry_point=main\&function_memory=256\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/javascript/cr-uploader/init.json"

# Upload Python functions.
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=pyup\&function_language=java\&function_entry_point=com.uploader.Uploader\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/python/gv-uploader/build/libuploader.so"
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=pyupow\&function_language=python\&function_entry_point=main\&function_memory=256\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/python/cr-uploader/init.json"

curl -s -X POST localhost:30009/upload_function?username=user\&function_name=pyco\&function_language=java\&function_entry_point=com.compression.Compression\&function_memory=256\&function_runtime=graalvisor\&function_isolation=false\&invocation_collocation=true -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/python/gv-compression/build/libcompression.so"
curl -s -X POST localhost:30009/upload_function?username=user\&function_name=pycoow\&function_language=python\&function_entry_point=main\&function_memory=256\&function_runtime=openwhisk\&function_isolation=true\&invocation_collocation=false -H 'Content-Type: application/octet-stream' --data-binary "$ARGO_HOME/../benchmarks/src/python/cr-compression/init.json"


# Make requests to Java functions.
curl -s -X POST localhost:30009/user/jvfh -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
curl -s -X POST localhost:30009/user/jvfhow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

curl -s -X POST localhost:30009/user/jvhr -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
curl -s -X POST localhost:30009/user/jvhrow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

# Make requests to JavaScript functions.
curl -s -X POST localhost:30009/user/jsdh -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/template.html","username":"user","nsize":"10"}'
curl -s -X POST localhost:30009/user/jsdhow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/template.html","username":"user","nsize":"10"}'

curl -s -X POST localhost:30009/user/jsup -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
curl -s -X POST localhost:30009/user/jsupow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

# Make requests to Python functions.
curl -s -X POST localhost:30009/user/pyup -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'
curl -s -X POST localhost:30009/user/pyupow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/snap.png"}'

curl -s -X POST localhost:30009/user/pyco -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/video.mp4"}'
curl -s -X POST localhost:30009/user/pycoow -H 'Content-Type: application/json' --data '{"url":"http://172.18.0.1:8000/video.mp4"}'
