#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../shared.sh

# Note: using "*_socket" functions in this script to run/stop LM with socket server.
start_lambda_manager_socket $(DIR)/config.json $(DIR)/variables.json
sleep 5

# Run the socket server client in an interactive mode; enter "close" (without quotes) to quit.
bash $ARGO_HOME/scheduler/fake-worker/run-client.sh

stop_lambda_manager_socket
