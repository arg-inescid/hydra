mvn clean package

PATH_TO_RESOURCES=""
VIRTUALIZE_PATH=$PATH_TO_RESOURCES/resources/virtualize.json

sudo env "PATH=$PATH" native-image \
  --no-fallback \
  --language:js \
  --language:python \
  --features=org.graalvm.argo.proxies.engine.PolyglotEngineSingletonFeature \
  -cp target/lambda-java-proxy-0.0.1.jar \
  org.graalvm.argo.proxies.PolyglotProxy \
  polyglot-proxy \
  -H:Virtualize="$VIRTUALIZE_PATH" \
  -H:+ReportExceptionStackTraces \
  -H:ConfigurationFileDirectories=src/main/resources/META-INF/native-image/
