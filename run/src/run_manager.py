#!/usr/bin/python3
import os.path
import sys
import threading
from time import sleep

import requests

from run_utils import print_message, MessageType, read_file
from run_globals import LAMBDA_MANAGER_DIR, PROXY_DIR


# Core methods.
def check_if_proxy_exists():
    print_message("Build all proxies if they are not exists...", MessageType.INFO)
    # Only one proxy to build
    os.system("bash {source_path}/build_proxy.sh".format(source_path=os.path.join(PROXY_DIR)))
    print_message("Build all proxies if they are not exists...done", MessageType.INFO)


def build_and_run():
    print_message("Build and run...", MessageType.INFO)
    os.system("bash {source_path}/build_and_run.sh".format(source_path=LAMBDA_MANAGER_DIR))
    print_message("Build and run...done", MessageType.INFO)


def upload(lambda_manager_config_path):
    print_message("Starting upload manager configuration daemon...", MessageType.INFO)

    # Wait for proxy and manager build before start polling.
    sleep(10)

    # Polling. We are waiting for lambda manager to become serviceable.
    for i in range(20):
        try:
            requests.post("http://localhost:9000/configure_manager",
                          headers={'Content-type': 'application/json'},
                          data=read_file(lambda_manager_config_path))
            return
        except requests.exceptions.ConnectionError:
            # Time between two checks.
            sleep(1)

    print_message("Daemon fail to upload configuration due to lambda manager inactivity!", MessageType.ERROR)


def upload_manager_configuration(lambda_manager_config_path):
    upload_thread = threading.Thread(target=upload, args=(lambda_manager_config_path,))
    upload_thread.start()


def do(lambda_manager_config_path):
    check_if_proxy_exists()
    if len(lambda_manager_config_path) > 0:
        upload_manager_configuration(lambda_manager_config_path)
    build_and_run()


# Main function.
def main(args):
    lambda_manager_config_path = ""
    if len(args) == 0:
        pass
    elif len(args) == 1:
        lambda_manager_config_path = os.path.join(os.path.curdir, args[0])
    else:
        print_message("Too much arguments - {}!".format(len(args)), MessageType.ERROR)
        exit(1)

    do(lambda_manager_config_path)


if __name__ == '__main__':
    main(sys.argv[1:])
