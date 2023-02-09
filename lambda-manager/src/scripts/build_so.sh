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

BUILD_OUTPUT_HOME="$FUNCTION_HOME"/build_so
mkdir -p "$BUILD_OUTPUT_HOME"
cd "$BUILD_OUTPUT_HOME" || {
  echo "Fail to create new directory for build output - $BUILD_OUTPUT_HOME!"
  exit 2
}

if [ ! -f "$PROXY_JAR" ]; then
  echo "Proxy JAR - $PROXY_JAR - not found!"
  exit 1
fi

FUNCTION_CODE=$FUNCTION_HOME/$FUNCTION_NAME
LAMBDA_HOME=$CODEBASE_HOME/lambda_"$LAMBDA_ID"_HOTSPOT_W_AGENT

"$JAVA_HOME"/bin/native-image \
  --no-fallback \
  -H:IncludeResources="logback.xml|application.yml" \
  -cp "$FUNCTION_CODE" \
  -DGraalVisorGuest=true \
  -Dcom.oracle.svm.graalvisor.libraryPath=$PROXY_HOME/build/resources/main/com.oracle.svm.graalvisor.headers \
  --initialize-at-run-time=com.oracle.svm.graalvisor.utils.JsonUtils \
  -H:ConfigurationFileDirectories="$LAMBDA_HOME"/shared/config \
  -H:+ReportExceptionStackTraces \
  --shared \
  -H:Name=lib"$FUNCTION_NAME" \
  -H:ExcludeResources=".*/io.micronaut.*$|io.netty.*$"

# TODO - we need to reintroduce the fallback feature (-H:+InterceptReflectiveOperationException)
