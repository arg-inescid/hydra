#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

OLD_DIR=$DIR
source "$DIR"/../lambda-manager/src/scripts/environment.sh
DIR=$OLD_DIR

GREEN='\033[0;32m'
NC='\033[0m' # No Color
FLAG=$1

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

if [[ -z "$FLAG" || "$FLAG" = "--java" ]]; then
    echo -e "${GREEN}Building java lambda proxy...${NC}"
    ./gradlew clean shadowJar
    echo -e "${GREEN}Building java lambda proxy...done${NC}"
fi

if [[ -z "$FLAG" || "$FLAG" = "--polyglot" ]]; then
    echo -e "${GREEN}Building polyglot lambda proxy...${NC}"

    cd "$TRUFFLE_HOME" || {
      echo "Redirection failed!"
      exit 1
    }

    $JAVA_HOME/bin/native-image \
      --no-fallback \
      --language:js \
      --language:python \
      --features=org.graalvm.argo.lambda_proxy.engine.PolyglotEngineSingletonFeature \
      -cp $PROXY_HOME/build/libs/lambda-proxy-1.0-all.jar \
      org.graalvm.argo.lambda_proxy.PolyglotProxy \
      polyglot-proxy \
      -H:Virtualize=$RES_HOME/virtualize-polyglot.json \
      -H:+ReportExceptionStackTraces \
      -H:ConfigurationFileDirectories=$TRUFFLE_HOME/META-INF/native-image/

    echo -e "${GREEN}Building polyglot lambda proxy...done${NC}"
fi
