#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
source "$DIR"/environment.sh

FUNCTION_NAME=$1
if [ -z "$FUNCTION_NAME" ]; then
  echo "Function name is not present."
  exit 1
fi

FUNCTION_HOME=$CODEBASE_HOME/$FUNCTION_NAME
FUNCTION_JAR=$FUNCTION_HOME/$FUNCTION_NAME.jar

cd "$FUNCTION_HOME" || {
  echo "Path [ $FUNCTION_HOME ] not found!"
  exit 1
}
if [[ ! -f $FUNCTION_HOME/$FUNCTION_NAME.img ]]; then
  "$JAVA_HOME"/bin/native-image \
    --no-fallback \
    -H:IncludeResources="logback.xml|application.yml" \
    -jar "$FUNCTION_JAR" \
    -H:Virtualize="$VIRTUALIZE_PATH" \
    -H:ConfigurationFileDirectories="$FUNCTION_HOME"/config \
    -H:ExcludeResources=".*/io.micronaut.*$|io.netty.*$"
fi
