#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}
./gradlew clean assemble
echo BENCHMARK_PATH="$DIR"/build/libs/array-hashing-1.0-all.jar
