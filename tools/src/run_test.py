#!/usr/bin/python3
import enum
import json
import os
import random
import re
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
    NO_HEADER = ""


# Test global variables.
MANAGER_LOG_FILE = os.path.join("..", "..", "lambda-manager", "manager_logs", "lambda_manager.log")
MAX_VERBOSITY_LVL = 2
VERBOSITY_LVL = 0
GENERAL_INFO = "general"

BENCHMARK_BUILD_SCRIPT = "build_script.sh"


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


def set_verbosity(flag):
    global VERBOSITY_LVL
    v_count = flag.count("v")
    if v_count != len(flag):
        print_message(GENERAL_INFO, "Verbosity flag should be v or vv instead of {flag}. Output verbosity level will "
                                    "fallback to 0."
                      .format(flag=flag), MessageType.WARN)
        return
    VERBOSITY_LVL = min(v_count, MAX_VERBOSITY_LVL)
    print_message(GENERAL_INFO, "Output verbosity level is set to {level}.".format(level=VERBOSITY_LVL),
                  MessageType.SPEC)


def install_required_packages():
    print_message(GENERAL_INFO, "Installing required packages...", MessageType.INFO)
    run(GENERAL_INFO, sys.executable + " -m pip install requests")
    print_message(GENERAL_INFO, "Installing required packages...done", MessageType.INFO)


# File util methods.
def load_data(filename):
    try:
        with open(filename) as input_file:
            return json.load(input_file)
    except IOError:
        print_message(GENERAL_INFO, "Input file {} is missing or deleted!".format(filename), MessageType.ERROR)
        exit(1)
    except JSONDecodeError as err:
        print_message(GENERAL_INFO, "Bad JSON syntax: {}".format(err), MessageType.ERROR)
        exit(1)


def read_file(username, filename, read_type='rb'):
    try:
        with open(filename, read_type) as input_file:
            return input_file.read()
    except IOError:
        print_message(username, "Input file {} is missing or deleted!".format(filename), MessageType.ERROR)
        exit(1)


def write_file(username, filename, content):
    try:
        with open(filename, 'a') as output_file:
            output_file.write(content)
    except IOError:
        print_message(username, "Error during writing in output file {}!".format(filename), MessageType.ERROR)
        exit(1)


# Core methods.
def run(username, command):
    outs, errs = subprocess.Popen(shlex.split(command),
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
    outs, errs = outs.decode(sys.stdout.encoding), errs.decode(sys.stdout.encoding)
    if len(outs) > 0 and VERBOSITY_LVL > 1:
        print_message(username, outs, MessageType.NO_HEADER)
    if len(errs) > 0 and VERBOSITY_LVL > 0:
        print_message(username, "Command ({}) error log:\n{}".format(command, errs), MessageType.WARN)
    return outs


def configure_managers(test_config_dir, managers):
    for manager in managers:
        print_message(GENERAL_INFO, "Response: " +
                      requests.post("{manager}/configure_manager".format(manager=manager['address']),
                                    headers={'Content-type': 'application/json'},
                                    data=read_file(GENERAL_INFO,
                                                   os.path.join(test_config_dir, manager['config_path']))).text,
                      MessageType.INFO)


def register_managers(load_balancer, managers):
    for manager in managers:
        print_message(GENERAL_INFO, "Response: " +
                      requests.post("{load_balancer}/register_manager?upstream=manager_cluster&add=&server={manager}"
                                    .format(load_balancer=load_balancer, manager=manager)).text, MessageType.INFO)


def build_benchmark(username, source_root):
    print_message(username, "Building benchmark...", MessageType.INFO)
    output = run(username,
                 "bash {source_path}/{script_name}".format(source_path=os.path.abspath(source_root),
                                                           script_name=BENCHMARK_BUILD_SCRIPT))
    benchmark_path = None
    try:
        benchmark_path = re.search('BENCHMARK_PATH=(.+)', output).group(1)
    except AttributeError:
        print_message(username, "BENCHMARK_PATH variable is not specified!", MessageType.ERROR)
        exit(2)
    print_message(username, "Building benchmark...done", MessageType.INFO)
    return benchmark_path


def upload_function(test_config_dir, username, entry_point, command_info):
    benchmark_path = build_benchmark(username, os.path.join(test_config_dir, command_info['source_root']))
    arguments = "&arguments=" + command_info['arguments'] if len(command_info['arguments']) > 0 else ""
    print_message(username, "Response: " +
                  requests.post("{entry_point}/upload_function?"
                                "allocate={allocate}&"
                                "username={username}&"
                                "function_name={function_name}&"
                                "function_language={function_language}&"
                                "function_entry_point={function_entry_point}"
                                "{arguments}"
                                .format(allocate=command_info['allocate'],
                                        entry_point=entry_point,
                                        username=username,
                                        function_name=command_info['function_name'],
                                        function_language=command_info['function_language'],
                                        function_entry_point=command_info['function_entry_point'],
                                        arguments=arguments),
                                headers={'Content-type': 'application/octet-stream'},
                                data=read_file(username, benchmark_path)).text, MessageType.INFO)


def remove_function(username, entry_point, command_info):
    print_message(username, "Response: " + requests.post("{entry_point}/remove_function?"
                                                         "username={username}&"
                                                         "function_name={function_name}"
                                                         .format(entry_point=entry_point,
                                                                 username=username,
                                                                 function_name=command_info['function_name'])).text,
                  MessageType.INFO)


def pause(username, command_info):
    print_message(username, "Pausing...", MessageType.INFO)
    time.sleep(command_info['duration'])
    print_message(username, "Pausing...done", MessageType.INFO)


def send(test_config_dir, username, manager, command_info):
    real_output_path = os.path.join(test_config_dir, command_info['output'])
    path = os.path.dirname(real_output_path)
    if len(path) > 0:
        os.makedirs(path, exist_ok=True)
    if os.path.exists(real_output_path):
        os.remove(real_output_path)

    parameters_len = len(command_info['parameters_pool'])
    for i in range(command_info['iterations']):
        print_message(username, "Iteration {} of {}...".format(i, command_info['iterations'] - 1), MessageType.INFO)

        parameters = "?parameters=" + command_info['parameters_pool'][random.randint(0, parameters_len - 1)] \
            if parameters_len > 0 else ""
        output = "ITERATION({})...\n".format(i)
        output += run(username,
                      "ab -n {num_requests} -c {num_clients} {manager}/{username}/{function_name}{parameters}"
                      .format(num_requests=command_info['num_requests'],
                              num_clients=command_info['num_clients'],
                              manager=manager,
                              username=username,
                              function_name=command_info['function_name'],
                              parameters=parameters))
        output += "ITERATION({})...done\n\n".format(i)
        write_file(username, real_output_path, output)

        print_message(username, "Iteration {} of {}...done".format(i, command_info['iterations'] - 1), MessageType.INFO)


def start_sending(test_config_dir, username, manager, command_info):
    send_threads = []
    for send_info in command_info['sending_info']:
        send_thread = threading.Thread(target=send, args=(test_config_dir, username, manager, send_info))
        send_thread.start()
        send_threads.append(send_thread)

    for send_thread in send_threads:
        send_thread.join()


def check_failure_pattern(username, failure_pattern):
    lambda_manager_log = read_file(username, MANAGER_LOG_FILE, 'r')
    if len(lambda_manager_log) == 0:
        print_message(username, "Lambda manager log is empty!", MessageType.WARN)
        return
    regex = re.compile(failure_pattern)
    found_failure_patterns = regex.findall(lambda_manager_log)
    if len(found_failure_patterns) > 0:
        print_message(username, "Failure pattern is found {} time(s)!".format(len(found_failure_patterns)),
                      MessageType.WARN)
    else:
        print_message(username, "Success pattern is found!", MessageType.SPEC)


def create_user(test_config_dir, user_info, manager):
    print_message(user_info['username'], "{} is sending commands...".format(user_info['username']), MessageType.INFO)

    kindness_counter = 0
    for command_info in user_info['commands']:
        command_info['command'].lower()
        if command_info['command'] == "u" or command_info['command'] == "upload":
            upload_function(test_config_dir, user_info['username'], manager, command_info)
            continue
        if command_info['command'] == "r" or command_info['command'] == "remove":
            remove_function(user_info['username'], manager, command_info)
            continue
        if command_info['command'] == "s" or command_info['command'] == "send":
            start_sending(test_config_dir, user_info['username'], manager, command_info)
            continue
        if command_info['command'] == "p" or command_info['command'] == "pause":
            pause(user_info['username'], command_info)
            continue
        if command_info['command'] == "k" or command_info['command'] == "kindness":
            kindness_counter += 1
    if kindness_counter > 0:
        print_message(user_info['username'], "Thank you, {}, for your kindness!".format(user_info['username']),
                      MessageType.SPEC)

    check_failure_pattern(user_info['username'], user_info['failure_pattern'])

    print_message(user_info['username'], "{} is sending commands...done".format(user_info['username']),
                  MessageType.INFO)


def unregister_managers(load_balancer, managers):
    for manager in managers:
        print_message(GENERAL_INFO, "Response: " +
                      requests.post("{load_balancer}/register_manager?upstream=manager_cluster&remove=&server={manager}"
                                    .format(load_balancer=load_balancer, manager=manager)).text, MessageType.INFO)


def test(test_config_dir, data):
    print_message(GENERAL_INFO, "Test - {} - is running...".format(data['test']), MessageType.INFO)

    # register_managers(data['entry_point'], data['managers'])
    configure_managers(test_config_dir, data['managers'])

    users = []
    for user_info in data['users']:
        user = threading.Thread(target=create_user, args=(test_config_dir, user_info, data['entry_point']))
        user.start()
        users.append(user)

    for user in users:
        user.join()

    # unregister_managers(data['entry_point'], data['managers'])

    print_message(GENERAL_INFO, "Test - {} - is running...done".format(data['test']), MessageType.INFO)


# Main function.
def main(args):
    global MANAGER_LOG_FILE
    MANAGER_LOG_FILE = os.path.join(os.path.dirname(sys.argv[0]), MANAGER_LOG_FILE)

    if len(args) == 0:
        print_message(GENERAL_INFO, "Insufficient number of arguments - {}!".format(len(args)), MessageType.ERROR)
        exit(1)
    test_config_index = 0
    if len(args) == 1:
        test_config_index = 0
        print_message(GENERAL_INFO, "Output verbosity level will be 0.", MessageType.SPEC)
    elif len(args) == 2:
        test_config_index = 1
        set_verbosity(args[0])
    else:
        print_message(GENERAL_INFO, "Too much arguments - {}!".format(len(args)), MessageType.ERROR)
        exit(1)

    install_required_packages()
    test(os.path.dirname(args[test_config_index]), load_data(args[test_config_index]))


if __name__ == '__main__':
    main(sys.argv[1:])
