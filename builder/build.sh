#!/bin/bash

# This script creates a Docker image that can be used to:
# 1. Build Native Image binaries;
# 2. Run HotSpot-based lambdas.

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

docker build --tag=argo-builder $DIR
