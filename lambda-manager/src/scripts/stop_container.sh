#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"


LAMBDA_NAME=$1
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

# We need to provide specific SIGTERM signal for HotSpot with Agent in order to allow
# the agent to write configuration properly. Graalvisor-based lambdas do not terminate
# when SIGTERM is provided.
IS_HOTSPOT=$2
if [[ "$IS_HOTSPOT" = "true" ]]; then
  SIGNAL_OPTION="--signal=SIGTERM"
fi

docker container kill $SIGNAL_OPTION $LAMBDA_NAME
