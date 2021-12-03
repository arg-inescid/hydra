JAVA_HOME=/home/ubuntu/git/graalvm-argo/argo/resources/graalvm-b655ac5f30-java11-21.3.0-dev
RES_HOME="/home/ubuntu/git/graalvm-argo/argo/resources"
TRUFFLE_HOME=$RES_HOME/truffle-build
PROXY_HOME=$RES_HOME/../lambda-proxy

cd $PROXY_HOME
./gradlew clean shadowJar
cd -

cd $TRUFFLE_HOME

sudo env "PATH=$PATH" $JAVA_HOME/bin/native-image \
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
cd -
