#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh
source "$DIR"/export_lambda_arguments.sh
source "$DIR"/prepare_lambda_directories.sh

export_lambda_arguments "${@:1:9}"
# shellcheck disable=SC2153
FUNCTION_HOME=$CODEBASE_HOME/"$FUNCTION_NAME"
FUNCTION_JAR=$FUNCTION_HOME/$FUNCTION_NAME.jar
LAMBDA_HOME=$FUNCTION_HOME/pid_"$LAMBDA_ID"_hotspot_w_agent
prepare_hotspot_lambda_directory "$LAMBDA_HOME"

PREV_AGENT_PID=$9
if [ -z "$PREV_AGENT_PID" ]; then
  echo "Previous agent pid is not present."
  exit 1
fi

PROXY_JAR=$PROXIES_HOME/java/target/lambda-java-proxy-0.0.1.jar
if [ ! -f $PROXY_JAR ]; then
  echo "Proxy JAR - $PROXY_JAR - not found!"
  exit 1
fi

cp "$PROXY_JAR" "$FUNCTION_JAR" "$LAMBDA_HOME"/shared
cp "$MANAGER_HOME"/src/main/resources/caller-filter-config.json "$LAMBDA_HOME"/shared

PREV_AGENT_CONFIG=$FUNCTION_HOME/pid_"$PREV_AGENT_PID"_hotspot_w_agent/shared/config
if [[ -d "$PREV_AGENT_CONFIG" ]]; then
  cp -r "$PREV_AGENT_CONFIG" "$LAMBDA_HOME"/shared/config
else
  mkdir "$LAMBDA_HOME"/shared/config
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/jni-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/predefined-classes-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/proxy-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/reflect-config.json
  printf "{\n}\n" > "$LAMBDA_HOME"/shared/config/resource-config.json
  printf "[\n]\n" > "$LAMBDA_HOME"/shared/config/serialization-config.json
fi

echo "\$JAVA_HOME/bin/java -Djava.library.path=\$JAVA_HOME/lib -agentlib:native-image-agent=config-merge-dir=config,caller-filter-file=caller-filter-config.json,report-dynamic-feature-failures -cp lambda-java-proxy-0.0.1.jar:$FUNCTION_NAME.jar org.graalvm.argo.proxies.JavaProxy ${*:10}" >"$LAMBDA_HOME"/shared/run.sh

"$LAMBDA_HOME"/debian_vm_unikernel.sh --memory "$LAMBDA_MEMORY" --gateway "$LAMBDA_GATEWAY" --ip "$LAMBDA_IP" \
  --mask "$LAMBDA_MASK" --kernel "$KERNEL_PATH" --img "$LAMBDA_HOME"/stretch.img --shared "$LAMBDA_HOME"/shared \
  --tap "$LAMBDA_TAP" "$LAMBDA_CONSOLE"
