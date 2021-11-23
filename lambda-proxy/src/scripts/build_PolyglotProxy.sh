./gradlew clean shadowJar

PATH_TO_RESOURCES=""
VIRTUALIZE_PATH=$PATH_TO_RESOURCES/resources/virtualize.json

sudo env "PATH=$PATH" native-image \
  --no-fallback \
  --language:js \
  --language:python \
  --features=org.graalvm.argo.lambda_proxy.engine.PolyglotEngineSingletonFeature \
  -cp build/libs/lambda-proxy-1.0.jar \
  org.graalvm.argo.lambda_proxy.PolyglotProxy \
  polyglot-proxy \
  -H:Virtualize="$VIRTUALIZE_PATH" \
  -H:+ReportExceptionStackTraces \
  -H:ConfigurationFileDirectories=src/main/resources/META-INF/native-image/
