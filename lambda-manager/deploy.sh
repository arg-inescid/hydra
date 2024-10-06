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

# Can be empty; in this case, a normal HTTP server will be launched.
SOCKET=$1
DEFAULT_LAMBDA_MANAGER_CONFIG="$ARGO_HOME/run/configs/manager/default-lambda-manager.json"

sudo java -jar build/libs/lambda-manager-1.0-all.jar $DEFAULT_LAMBDA_MANAGER_CONFIG $SOCKET
