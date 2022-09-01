#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/../lambda-manager/src/scripts/environment.sh

$JAVA_HOME/bin/gu install native-image python nodejs

VENV=$JAVA_HOME/graalvisor-python-venv

$JAVA_HOME/bin/graalpython -m venv $VENV
source $VENV/bin/activate

graalpython -m ginstall list
graalpython -m ginstall install numpy
graalpython -m ginstall install Pillow
graalpython -m ginstall install requests
