#!/usr/bin/python3
import enum
import json
import os
import random
import shlex
import subprocess
import sys
import threading
import time
from json import JSONDecodeError

import requests


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO-{}]"
    ERROR = "[ERR0-{}]"
    WARN = "[WARN-{}]"
    SPEC = "[SPEC-{}]"


# Util methods.
def print_message(username, message, t):
    if t == MessageType.INFO:
        print('\033[1;39m' + t.value.format(username) + " " + message + '\033[0m')  # default
        return
    if t == MessageType.ERROR:
        print('\033[1;31m' + t.value.format(username) + " " + message + '\033[0m')  # red
        return
    if t == MessageType.WARN:
        print('\033[1;33m' + t.value.format(username) + " " + message + '\033[0m')  # yellow
        return
    if t == MessageType.SPEC:
        print('\033[1;32m' + t.value.format(username) + " " + message + '\033[0m')  # green
        return
    print(message)


def install_required_packages():
    required_packages = subprocess.check_output([sys.executable, '-m', 'pip', 'freeze'])
    installed_packages = [r.decode().split('==')[0] for r in required_packages.split()]
    print_message("general", "Installed packages: [ " + " ".join(installed_packages) + " ]", MessageType.WARN)


# File util methods.
def load_data(filename):
    try:
        with open(filename) as input_file:
            return json.load(input_file)
    except IOError:
        print_message("general", "Input file {} is missing or deleted!".format(filename), MessageType.ERROR)
        exit(1)
    except JSONDecodeError as err:
        print_message("general", "Bad JSON syntax: {}".format(err), MessageType.ERROR)
        exit(1)


def read_file(username, filename):
    try:
        with open(filename, 'rb') as input_file:
            return input_file.read()
    except IOError:
        print_message(username, "Input file {} is missing or deleted!".format(filename), MessageType.WARN)


def write_file(username, filename, output):
    try:
        with open(filename, 'a') as output_file:
            output_file.write(output)
    except IOError:
        print_message(username, "Error during writing in output file {}!".format(filename), MessageType.WARN)


# Core methods.
def run(username, command):
    outs, errs = subprocess.Popen(shlex.split(command),
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
    outs, errs = outs.decode(sys.stdout.encoding), errs.decode(sys.stdout.encoding)
    if len(errs) > 0:
        print_message(username, "Command ({}) error log:\n{}".format(command, errs), MessageType.WARN)
    return outs


def upload_lambdas(username, manager, command):
    for _ in range(command['num_lambdas']):
        print_message(username, "Response: " +
                      requests.post("{manager}/upload_lambda?user={username}&name={lambda_name}"
                                    .format(manager=manager, username=username, lambda_name=command['lambda_name']),
                                    headers={'Content-type': 'application/octet-stream'},
                                    data=read_file(username, command['res_path'])).text, MessageType.INFO)


def remove_lambdas(username, manager, lambda_name):
    print_message(username, "Response: " + requests.post("{manager}/remove_lambda?user={username}&name={lambda_name}"
                                                         .format(manager=manager, username=username,
                                                                 lambda_name=lambda_name)).text,
                  MessageType.INFO)


def send(username, manager, command):
    path = os.path.dirname(command['output_file'])
    if len(path) > 0:
        os.makedirs(path, exist_ok=True)
    if os.path.exists(command['output_file']):
        os.remove(command['output_file'])

    args_len = len(command['lambda_args'])
    for i in range(command['iterations']):
        print_message(username, "Iteration {} of {}...".format(i + 1, command['iterations']), MessageType.INFO)

        args = "?args=" + command['lambda_args'][random.randint(0, args_len - 1)] if args_len > 0 else ""
        output = "ITERATION({})...\n".format(i)
        output += run(username, "ab -n {num_requests} -c {num_clients} {manager}/{username}/{lambda_name}{args}"
                      .format(num_requests=command['num_requests'], num_clients=command['num_clients'],
                              manager=manager, username=username, lambda_name=command['lambda_name'],
                              args=args))
        output += "ITERATION({})...done\n\n".format(i)
        write_file(username, command['output_file'], output)

        print_message(username, "Iteration {} of {}...done".format(i + 1, command['iterations']), MessageType.INFO)


def star_sending(username, manager, sending_info):
    send_threads = []
    for one_send_info in sending_info:
        if len(one_send_info) < 6:
            print_message(username, "Insufficient number of arguments for request command ({})!"
                          .format(len(one_send_info)), MessageType.WARN)
            continue
        send_thread = threading.Thread(target=send, args=(username, manager, {
            "iterations": one_send_info[0],
            "num_requests": one_send_info[1],
            "num_clients": one_send_info[2],
            "lambda_name": one_send_info[3],
            "lambda_args": one_send_info[4],
            "output_file": one_send_info[5]
        }))
        send_thread.start()
        send_threads.append(send_thread)

    for send_thread in send_threads:
        send_thread.join()


def start_user(user, manager):
    print_message(user['username'], "{} is running...".format(user['username']), MessageType.INFO)

    kindness_counter = 0
    for command in user['commands']:
        comm_len = len(command)
        if comm_len < 1:
            print_message(user['username'], "Insufficient number of arguments for command ({})!".format(comm_len),
                          MessageType.WARN)
            continue
        command[0].lower()
        if command[0] == "u" or command[0] == "upload":
            if comm_len < 4:
                print_message(user['username'], "Insufficient number of arguments for upload command ({})!"
                              .format(comm_len), MessageType.WARN)
                continue
            upload_lambdas(user['username'], manager, {
                "num_lambdas": command[1],
                "lambda_name": command[2],
                "res_path": command[3],
            })
            continue
        if command[0] == "s" or command[0] == "send":
            if comm_len < 2:
                print_message(user['username'], "Insufficient number of arguments for send command ({})!"
                              .format(comm_len), MessageType.WARN)
                continue
            star_sending(user['username'], manager, command[1:])
            continue
        if command[0] == "p" or command[0] == "pause":
            if comm_len < 2:
                print_message(user['username'], "Insufficient number of arguments for pause command ({})!"
                              .format(comm_len), MessageType.WARN)
                continue
            print_message(user['username'], "Pausing...", MessageType.INFO)
            time.sleep(command[1])
            print_message(user['username'], "Pausing...done", MessageType.INFO)
            continue
        if command[0] == "r" or command[0] == "remove":
            if comm_len < 2:
                print_message(user['username'], "Insufficient number of arguments for remove lambda command ({})!"
                              .format(comm_len), MessageType.WARN)
                continue
            remove_lambdas(user['username'], manager, command[1])
            continue
        if command[0] == "k" or command[0] == "kindness":
            if comm_len < 2:
                print_message(user['username'], "Insufficient number of arguments for kindness command ({})!"
                              .format(comm_len), MessageType.WARN)
                continue
            kindness_counter += 1
    if kindness_counter > 0:
        print_message(user['username'], "Thank you, {}, for your kindness!".format(user['username']), MessageType.SPEC)

    print_message(user['username'], "{} is running...done".format(user['username']), MessageType.INFO)


def configure_manager(manager, config_path):
    print_message("general", "Response: " +
                  requests.post("{manager}/configure_manager".format(manager=manager),
                                headers={'Content-type': 'application/json'},
                                data=read_file("general", config_path)).text, MessageType.INFO)


def test(data):
    print_message("general", "{} is running...".format(data['test']), MessageType.INFO)
    configure_manager(data['manager'], data['config_path'])

    users = []
    for user in data['users']:
        user = threading.Thread(target=start_user, args=(user, data['manager']))
        user.start()
        users.append(user)

    for user in users:
        user.join()

    print_message("general", "{} is running...done".format(data['test']), MessageType.INFO)


# Main function.
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print_message("general", "Insufficient number of arguments ({})!".format(len(sys.argv)), MessageType.ERROR)
        exit(1)
    install_required_packages()
    test(load_data(sys.argv[1]))
