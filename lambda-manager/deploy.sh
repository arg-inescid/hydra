#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
	echo "ARGO_HOME is not defined. Exiting..."
	exit 1
fi

if [[ -z "${JAVA_HOME}" ]]; then
	echo "JAVA_HOME is not defined. Exiting..."
	exit 1
fi

cd "$ARGO_HOME/lambda-manager" || {
  echo "Redirection fails!"
  exit 1
}

sudo java -Xms8g -Xmx32g -jar build/libs/lambda-manager-1.0-all.jar
