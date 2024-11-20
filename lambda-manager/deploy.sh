#!/bin/bash

print_and_die() {
    echo -e "$1" >&2
    exit 1
}

USAGE=$(cat << USAGE_END
Usage: [--config /path/to/config.json] [--variables /path/to/variables.json] [--socket] [--http]
       --config /path/to/config.json       - Path to the node manager configuration
       --variables /path/to/variables.json - Path to the file containing variables used by the node manager
       --socket                            - Start a socket server (can be used together with an HTTP server)
       --http                              - Start an HTTP server (can be used together with a socket server)
USAGE_END
)

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

# Initialize parameters with default values or leave empty.
CONFIG_PATH="$ARGO_HOME/run/configs/manager/default-lambda-manager.json"
VARIABLES_PATH="$ARGO_HOME/run/configs/manager/default-variables.json"
SOCKET_SERVER_OPTION=
HTTP_SERVER_OPTION=

while :; do
    case $1 in
    -h | --help)
        print_and_die "$USAGE"
        ;;
    -c | --config)
        if [ "$2" ]; then
            CONFIG_PATH=$2
            shift
        else
            print_and_die "Flag --config requires an additional argument\n$USAGE"
        fi
        ;;
    --variables)
        if [ "$2" ]; then
            VARIABLES_PATH=$2
            shift
        else
            print_and_die "Flag --variables requires an additional argument\n$USAGE"
        fi
        ;;
    --socket)
        SOCKET_SERVER_OPTION="--socket"
        ;;
    --http)
        HTTP_SERVER_OPTION="--http"
        ;;
    *)
        break;
        ;;
    esac

    shift
done

$JAVA_HOME/bin/java -jar build/libs/lambda-manager-1.0-all.jar --config $CONFIG_PATH --variables $VARIABLES_PATH $SOCKET_SERVER_OPTION $HTTP_SERVER_OPTION
