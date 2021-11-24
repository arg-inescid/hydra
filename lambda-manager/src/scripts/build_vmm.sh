#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

FUNCTION_NAME=$1
if [ -z "$FUNCTION_NAME" ]; then
  echo "Function name is not present."
  exit 1
fi

LAMBDA_ID=$2
if [ -z "$LAMBDA_ID" ]; then
  echo "Lambda id is not present."
  exit 1
fi

FUNCTION_HOME=$CODEBASE_HOME/$FUNCTION_NAME
if [[ ! -d "$FUNCTION_HOME" ]]; then
  echo "Function - $FUNCTION_HOME - is not found!"
  exit 1
fi

BUILD_OUTPUT_HOME="$FUNCTION_HOME"/build_vmm
mkdir -p "$BUILD_OUTPUT_HOME"
cd "$BUILD_OUTPUT_HOME" || {
  echo "Fail to create new directory for build output - $BUILD_OUTPUT_HOME!"
  exit 2
}

if [ ! -f "$PROXY_JAR" ]; then
  echo "Proxy JAR - $PROXY_JAR - not found!"
  exit 1
fi

FUNCTION_JAR=$FUNCTION_HOME/$FUNCTION_NAME.jar
LAMBDA_HOME=$FUNCTION_HOME/pid_"$LAMBDA_ID"_hotspot_w_agent
"$JAVA_HOME"/bin/native-image \
  --no-fallback \
  --features=org.graalvm.argo.lambda_proxy.engine.JavaEngineSingletonFeature \
  -H:IncludeResources="logback.xml|application.yml" \
  -cp "$PROXY_JAR":"$FUNCTION_JAR" \
  org.graalvm.argo.lambda_proxy.JavaProxy \
  "$FUNCTION_NAME" \
  -H:Virtualize="$VIRTUALIZE_PATH" \
  -H:ConfigurationFileDirectories="$LAMBDA_HOME"/shared/config \
  -H:ExcludeResources=".*/io.micronaut.*$|io.netty.*$" \
  -H:+InterceptReflectiveOperationException
