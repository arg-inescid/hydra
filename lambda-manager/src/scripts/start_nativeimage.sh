#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
source $DIR/env.sh

FUNCTION_NAME=$1
if [ -z "$FUNCTION_NAME" ]
then
	echo "FUNCTION name is not present."
	exit 1
fi

LAMBDA_MEMORY=$2
if [ -z "$LAMBDA_MEMORY" ]
then
	echo "Lambda memory is not present."
	exit 1
fi

LAMBDA_IP=$3
if [ -z "$LAMBDA_IP" ]
then
	echo "Lambda ip is not present."
	exit 1
fi

LAMBDA_TAP=$4
if [ -z "$LAMBDA_TAP" ]
then
	echo "Lambda tap is not present."
	exit 1
fi

LAMBDA_GATEWAY=$5
if [ -z "$LAMBDA_GATEWAY" ]
then
	echo "Lambda gateway is not present."
	exit 1
fi

LAMBDA_MASK=$6
if [ -z "$LAMBDA_MASK" ]
then
	echo "Lambda mask is not present."
	exit 1
fi

FUNCTION_HOME=$MANAGER_HOME/src/lambdas/$FUNCTION_NAME

bash $FUNCTION_HOME/${FUNCTION_NAME}_unikernel.sh \
	--memory $LAMBDA_MEMORY \
	--ip $LAMBDA_IP \
	--tap $LAMBDA_TAP \
	--gateway $LAMBDA_GATEWAY \
	--mask $LAMBDA_MASK \
        ${@:7}
