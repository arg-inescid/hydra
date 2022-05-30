#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

OLD_DIR=$DIR
source "$DIR"/../lambda-manager/src/scripts/environment.sh
DIR=$OLD_DIR

GREEN='\033[0;32m'
NC='\033[0m' # No Color
FLAG=$1

LANGS=""
LANGS="$LANGS --language:js"
LANGS="$LANGS --language:python"

function build {
    sudo -E $JAVA_HOME/bin/native-image \
      --no-fallback \
      -DGraalVisorHost \
      -Dcom.oracle.svm.graalvisor.libraryPath=$PROXY_HOME/build/resources/main/com.oracle.svm.graalvisor.headers \
      $LANGS \
      --features=org.graalvm.argo.lambda_proxy.engine.PolyglotEngineSingletonFeature \
      -cp $PROXY_HOME/build/libs/lambda-proxy-1.0-all.jar \
      $MAINCLASS \
      polyglot-proxy \
      $VIRTUALIZE \
      -H:+ReportExceptionStackTraces \
      -H:ConfigurationFileDirectories=$TRUFFLE_HOME/META-INF/native-image/
}

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

echo -e "${GREEN}Building java lambda proxy...${NC}"
./gradlew clean shadowJar javaProxy
echo -e "${GREEN}Building java lambda proxy...done${NC}"

if [[ ! -z "$FLAG" ]]; then

    cd "$TRUFFLE_HOME" || {
      echo "Redirection failed!"
      exit 1
    }

    if [[ "$FLAG" = "--polyglot" ]]; then
        echo -e "${GREEN}Building polyglot lambda proxy...${NC}"
        VIRTUALIZE="-H:Virtualize=$VIRTUALIZE_PATH"
        MAINCLASS="org.graalvm.argo.lambda_proxy.PolyglotProxy"
        build
        echo -e "${GREEN}Building polyglot lambda proxy...done${NC}"
    elif [[ "$FLAG" = "--polyglot-baremetal" ]]; then
        echo -e "${GREEN}Building baremetal polyglot lambda proxy...${NC}"
        VIRTUALIZE=""
        MAINCLASS="org.graalvm.argo.lambda_proxy.BaremetalPolyglotProxy"
        build
        echo -e "${GREEN}Building baremetal polyglot lambda proxy...done${NC}"
    fi
fi
