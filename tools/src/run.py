#!/usr/bin/python3
import enum
import sys

import run_install_deps
import run_plot
import run_test
import run_tests
import run_measurements


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO]"
    ERROR = "[ERR0]"
    WARN = "[WARN]"
    SPEC = "[SPEC]"
    NO_HEADER = ""


# Script type.
class ScriptType(enum.Enum):
    TEST = "test"
    PLOT = "plot"
    INSTALL_DEPS = "install-deps"
    TESTS = "tests"
    MEASUREMENTS = "measurements"
    HELP = "help"


# Util methods.
def print_message(message, t):
    if t == MessageType.INFO:
        print('\033[1;39m' + t.value + " " + message + '\033[0m')  # default
        return
    if t == MessageType.ERROR:
        print('\033[1;31m' + t.value + " " + message + '\033[0m')  # red
        return
    if t == MessageType.WARN:
        print('\033[1;33m' + t.value + " " + message + '\033[0m')  # yellow
        return
    if t == MessageType.SPEC:
        print('\033[1;32m' + t.value + " " + message + '\033[0m')  # green
        return
    print(message)


def print_help_message():
    print('''
Argo's command-line tool.
    
The Run helps you with building, testing, plotting, installing dependencies...
 
    test [v|vv] <path to test configuration>
                      allow you to run test with specified verbosity level and test configuration.
    tests [v|vv]       
                      allow you tu run all tests for all tiers with given verbosity.
    plot [v|vv] <path to plot configuration>
                      create plots based on configuration with given verbosity.
    install-deps      install all necessary dependencies.
    help              print help and exit.
    ''')


def print_error_message():
    print_message("Command not found. Try help?", MessageType.ERROR)


def parse_input_arguments(args):
    if args[0] == ScriptType.TEST.value:
        run_test.main(args[1:])
        return
    if args[0] == ScriptType.PLOT.value:
        run_plot.main(args[1:])
        return
    if args[0] == ScriptType.INSTALL_DEPS.value:
        run_install_deps.main(args[1:])
        return
    if args[0] == ScriptType.TESTS.value:
        run_tests.main(args[1:])
        return
    if args[0] == ScriptType.MEASUREMENTS.value:
        run_measurements.main(args[1:])
        return
    if args[0] == ScriptType.HELP.value:
        print_help_message()
        return
    print_error_message()


# Main script for run command. Called from run.sh.
if __name__ == '__main__':
    parse_input_arguments(sys.argv[1:])
