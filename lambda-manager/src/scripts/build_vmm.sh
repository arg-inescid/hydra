#!/bin/bash

# 1. argument - native image location
# 2. argument - vmm code directory
# 3. argument - vmm code filename
# 4. argument - virtualization config filepath


cd "$2" || { echo "**** Path ($2) is missing! ****"; exit 1; }
"$1"/native-image -H:IncludeResources="logback.xml|application.yml" -jar "$3" \
  -H:Virtualize="$4" -H:ConfigurationFileDirectories=./config \
  -H:ExcludeResources=".*/io.micronaut.*$|io.netty.*$"
