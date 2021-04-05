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
    print_message("general", run("general", sys.executable + " -m pip install requests"), MessageType.WARN)


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
        print_message(username, "Input file {} is missing or deleted!".format(filename), MessageType.ERROR)
        exit(1)


def write_file(username, filename, output):
    try:
        with open(filename, 'a') as output_file:
            output_file.write(output)
    except IOError:
        print_message(username, "Error during writing in output file {}!".format(filename), MessageType.ERROR)
        exit(1)


# Core methods.
def run(username, command):
    outs, errs = subprocess.Popen(shlex.split(command),
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
    outs, errs = outs.decode(sys.stdout.encoding), errs.decode(sys.stdout.encoding)
    if len(errs) > 0:
        print_message(username, "Command ({}) error log:\n{}".format(command, errs), MessageType.WARN)
    return outs


def configure_managers(managers):
    for manager in managers:
        print_message("general", "Response: " +
                      requests.post("{manager}/configure_manager".format(manager=manager['address']),
                                    headers={'Content-type': 'application/json'},
                                    data=read_file("general", manager['config_path'])).text, MessageType.INFO)


def register_managers(load_balancer, managers):
    for manager in managers:
        print_message("general", "Response: " +
                      requests.post("{load_balancer}/register_manager?upstream=manager_cluster&add=&server={manager}"
                                    .format(load_balancer=load_balancer, manager=manager)).text, MessageType.INFO)


def upload_lambdas(username, entry_point, command_info):
    print_message(username, "Response: " +
                  requests.post("{entry_point}/upload_lambda?allocate={allocate}&user={username}&name={lambda_name}"
                                .format(allocate=command_info['allocate'], entry_point=entry_point, username=username,
                                        lambda_name=command_info['lambda_name']),
                                headers={'Content-type': 'application/octet-stream'},
                                data=read_file(username, command_info['source'])).text, MessageType.INFO)


def remove_lambdas(username, entry_point, command_info):
    print_message(username, "Response: " + requests.post("{entry_point}/remove_lambda?"
                                                         "user={username}&name={lambda_name}"
                                                         .format(entry_point=entry_point, username=username,
                                                                 lambda_name=command_info['lambda_name'])).text,
                  MessageType.INFO)


def pause(username, command_info):
    print_message(username, "Pausing...", MessageType.INFO)
    time.sleep(command_info['duration'])
    print_message(username, "Pausing...done", MessageType.INFO)


def send(username, manager, command_info):
    path = os.path.dirname(command_info['output'])
    if len(path) > 0:
        os.makedirs(path, exist_ok=True)
    if os.path.exists(command_info['output']):
        os.remove(command_info['output'])

    args_len = len(command_info['args_pool'])
    for i in range(command_info['iterations']):
        print_message(username, "Iteration {} of {}...".format(i, command_info['iterations'] - 1), MessageType.INFO)

        args = "?args=" + command_info['args_pool'][random.randint(0, args_len - 1)] if args_len > 0 else ""
        output = "ITERATION({})...\n".format(i)
        output += run(username, "ab -n {num_requests} -c {num_clients} {manager}/{username}/{lambda_name}{args}"
                      .format(num_requests=command_info['num_requests'], num_clients=command_info['num_clients'],
                              manager=manager, username=username, lambda_name=command_info['lambda_name'],
                              args=args))
        output += "ITERATION({})...done\n\n".format(i)
        write_file(username, command_info['output'], output)

        print_message(username, "Iteration {} of {}...done".format(i, command_info['iterations'] - 1), MessageType.INFO)


def start_sending(username, manager, command_info):
    send_threads = []
    for send_info in command_info['sending_info']:
        send_thread = threading.Thread(target=send, args=(username, manager, send_info))
        send_thread.start()
        send_threads.append(send_thread)

    for send_thread in send_threads:
        send_thread.join()


def create_user(user_info, manager):
    print_message(user_info['username'], "{} is running...".format(user_info['username']), MessageType.INFO)

    kindness_counter = 0
    for command_info in user_info['commands']:
        command_info['command'].lower()
        if command_info['command'] == "u" or command_info['command'] == "upload":
            upload_lambdas(user_info['username'], manager, command_info)
            continue
        if command_info['command'] == "r" or command_info['command'] == "remove":
            remove_lambdas(user_info['username'], manager, command_info)
            continue
        if command_info['command'] == "s" or command_info['command'] == "send":
            start_sending(user_info['username'], manager, command_info)
            continue
        if command_info['command'] == "p" or command_info['command'] == "pause":
            pause(user_info['username'], command_info)
            continue
        if command_info['command'] == "k" or command_info['command'] == "kindness":
            kindness_counter += 1
    if kindness_counter > 0:
        print_message(user_info['username'], "Thank you, {}, for your kindness!".format(user_info['username']),
                      MessageType.SPEC)

    print_message(user_info['username'], "{} is running...done".format(user_info['username']), MessageType.INFO)


def unregister_managers(load_balancer, managers):
    for manager in managers:
        print_message("general", "Response: " +
                      requests.post("{load_balancer}/register_manager?upstream=manager_cluster&remove=&server={manager}"
                                    .format(load_balancer=load_balancer, manager=manager)).text, MessageType.INFO)


def test(data):
    print_message("general", "{} is running...".format(data['test']), MessageType.INFO)

    # register_managers(data['entry_point'], data['managers'])
    configure_managers(data['managers'])

    users = []
    for user_info in data['users']:
        user = threading.Thread(target=create_user, args=(user_info, data['entry_point']))
        user.start()
        users.append(user)

    for user in users:
        user.join()

    # unregister_managers(data['entry_point'], data['managers'])

    print_message("general", "{} is running...done".format(data['test']), MessageType.INFO)


# Main function.
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print_message("general", "Insufficient number of arguments ({})!".format(len(sys.argv)), MessageType.ERROR)
        exit(1)
    install_required_packages()
    test(load_data(sys.argv[1]))
