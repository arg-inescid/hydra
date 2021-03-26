#!/bin/bash

# 1. argument - native image location
# 2. argument - vmm code filename
# 3. argument - virtualization config filepath

"$1"/native-image -H:IncludeResources="logback.xml|application.yml" -jar "$2" \
  -H:Virtualize="$3" -H:ConfigurationFileDirectories=./config \
  -H:ExcludeResources=".*/io.micronaut.*$|io.netty.*$"
