#!/bin/bash

# This script creates a Docker image that can be used to build Native Image binaries.

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

docker image rm argo-builder:latest
docker build --tag=argo-builder $DIR
