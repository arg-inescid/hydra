#!/bin/bash

# 1. argument - lambda code directory
# 2. argument - lambda code jar filename
# 3. argument - virtualization config file path

cd "$1" || exit
native-image -H:IncludeResources="logback.xml|application.yml" -jar "$2" \
  -H:Virtualize="$3" -H:ConfigurationFileDirectories=./config \
  -H:ExcludeResources=".*/io.micronaut.*$|io.netty.*$"
