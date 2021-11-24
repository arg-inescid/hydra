#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

cd "$DIR" || {
  echo "Redirection fails!"
  exit 1
}

sudo java -jar build/libs/lambda-manager-1.0-all.jar
