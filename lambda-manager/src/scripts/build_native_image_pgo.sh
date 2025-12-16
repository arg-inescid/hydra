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

ENTRY_POINT=$3
if [ -z "ENTRY_POINT" ]; then
  echo "Function entry point is not present."
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


ARGO_HOME="$MANAGER_HOME"/../
BUILD_OUTPUT_HOME_CONTAINER=/argo/lambda-manager/codebase/"$FUNCTION_NAME"/pgo-enable
FUNCTION_CODE_CONTAINER=/argo/lambda-manager/codebase/"$FUNCTION_NAME"/"$FUNCTION_NAME"
HYDRA_GUEST_JAR_CONTAINER=/argo/hydra-lib/build/libs/hydra-lib-1.0-guest.jar
HEADERS_PATH_CONTAINER=/argo/hydra/build/resources/main/com.oracle.svm.hydra.headers
LAMBDA_HOME_CONTAINER=/argo/lambda-manager/codebase/lambda_"$LAMBDA_ID"_HOTSPOT_W_AGENT

docker run --rm \
  -v "$JAVA_HOME":/jvm \
  -v "$ARGO_HOME":/argo \
  -w "$BUILD_OUTPUT_HOME_CONTAINER" \
  argo-builder \
    /jvm/bin/native-image \
      --no-fallback \
      -H:IncludeResources="logback.xml|application.yml" \
      -cp "$FUNCTION_CODE_CONTAINER":"$HYDRA_GUEST_JAR_CONTAINER" \
      -DHydraGuest=true \
      -Dcom.oracle.svm.hydra.libraryPath="$HEADERS_PATH_CONTAINER" \
      --initialize-at-run-time=com.oracle.svm.hydra.utils.JsonUtils \
      -H:ConfigurationFileDirectories="$LAMBDA_HOME_CONTAINER"/config \
      -H:+ReportExceptionStackTraces \
      "$ENTRY_POINT" \
      --pgo-instrument \
      -H:Name="$FUNCTION_NAME" \
      -H:ExcludeResources=".*/io.micronaut.*$|io.netty.*$"

## TODO - we need to reintroduce the fallback feature (-H:+InterceptReflectiveOperationException)
