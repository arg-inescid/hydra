#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

OLD_DIR=$DIR
source "$DIR"/../lambda-manager/src/scripts/environment.sh
DIR=$OLD_DIR

GREEN='\033[0;32m'
NC='\033[0m' # No Color
FLAG=$1

function build_ni {
    mkdir -p $GRAALVISOR_HOME &> /dev/null
    cd $GRAALVISOR_HOME
    $JAVA_HOME/bin/native-image \
        --no-fallback \
        --enable-url-protocols=http \
        -DGraalVisorHost \
        -Dcom.oracle.svm.graalvisor.libraryPath=$PROXY_HOME/build/resources/main/com.oracle.svm.graalvisor.headers \
        $LANGS \
        --features=org.graalvm.argo.lambda_proxy.engine.PolyglotEngineSingletonFeature \
        -cp $PROXY_HOME/build/libs/graalvisor-1.0-all.jar \
        org.graalvm.argo.lambda_proxy.PolyglotProxy \
        polyglot-proxy \
        -H:+ReportExceptionStackTraces \
        -H:ConfigurationFileDirectories=$PROXY_HOME/ni-agent-config/native-image,$PROXY_HOME/ni-agent-config/native-image-jvips
}

function build_niuk {
    $NIUK_HOME/build_niuk.sh $JAVA_HOME $GRAALVISOR_HOME/polyglot-proxy $GRAALVISOR_HOME/polyglot-proxy.img
}

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

LANGS=""
read -p "Javascript support (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    LANGS="$LANGS --language:js"
    echo "JavaScript support added!"
fi

read -p "Python support (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    LANGS="$LANGS --language:python"
    echo "Python support added!"
fi

echo -e "${GREEN}Building lambda proxy Jar...${NC}"
./gradlew clean shadowJar javaProxy
echo -e "${GREEN}Building lambda proxy Jar... done!${NC}"

echo -e "${GREEN}Building lambda proxy Native Image...${NC}"
build_ni
echo -e "${GREEN}Building lambda proxy Native Image... done!${NC}"

echo -e "${GREEN}Building lambda proxy Native Image Unikernel...${NC}"
build_niuk
echo -e "${GREEN}Building lambda proxy Native Image Unikernel... done!${NC}"
