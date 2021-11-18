mvn clean package

PATH_TO_CONFIGURATIONS=""

native-image --no-fallback \
  --features=org.graalvm.argo.proxies.engine.JavaEngineSingletonFeature \
  --trace-class-initialization=org.graalvm.argo.proxies.JavaProxy \
  -cp target/lambda-java-proxy-0.0.1.jar:../../benchmarks/language/java/hello-world/build/libs/hello-world-1.0.jar \
  org.graalvm.argo.proxies.JavaProxy \
  java-proxy \
  -H:+ReportExceptionStackTraces \
  -H:ConfigurationFileDirectories="$PATH_TO_CONFIGURATIONS"
