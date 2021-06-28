#!/usr/bin/python3
import enum
import os
import shlex
import subprocess
import sys


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO]"
    ERROR = "[ERR0]"
    WARN = "[WARN]"
    SPEC = "[SPEC]"
    NO_HEADER = ""


# Test global variables.
TESTING_DIR = os.path.join("..", "configs", "test")
MAX_VERBOSITY_LVL = 2
VERBOSITY_LVL = 0


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


def set_verbosity(flag):
    global VERBOSITY_LVL
    v_count = flag.count("v")
    if v_count != len(flag):
        print_message("Verbosity flag should be v or vv instead of {flag}. Output verbosity level will fallback to 0."
                      .format(flag=flag), MessageType.WARN)
        return
    VERBOSITY_LVL = min(v_count, MAX_VERBOSITY_LVL)
    print_message("Output verbosity level is set to {level}.".format(level=VERBOSITY_LVL), MessageType.SPEC)


# Core methods.
def run(command):
    outs, errs = subprocess.Popen(shlex.split(command),
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
    outs, errs = outs.decode(sys.stdout.encoding), errs.decode(sys.stdout.encoding)
    if len(outs) > 0 and VERBOSITY_LVL > 1:
        print_message(outs, MessageType.NO_HEADER)
    if len(errs) > 0 and VERBOSITY_LVL > 0:
        print_message("Command ({}) error log:\n{}".format(command, errs), MessageType.WARN)
    return outs


def test_all():
    for root, dirs, files in sorted(os.walk(TESTING_DIR)):
        # In case of first iteration where root is equals to configs/test.
        if root == TESTING_DIR:
            continue
        current_tier = root.split(os.path.sep)[-1].replace("-", " ")
        current_tier = current_tier[0].upper() + current_tier[1:]
        print_message("{}...".format(current_tier), MessageType.INFO)
        for file in sorted(files):
            if file == ".gitkeep":
                continue
            print_message("Test - {} - is running...".format(file.split(".json")[0]), MessageType.INFO)
            run("python3 run-test.py {verbosity} {config}".format(verbosity="v" * VERBOSITY_LVL,
                                                                  config=os.path.join(root, file)))
            print_message("Test - {} - is running...done".format(file.split(".json")[0]), MessageType.INFO)
        print_message("{}...done".format(current_tier), MessageType.INFO)


# Main function.
if __name__ == '__main__':
    if len(sys.argv) == 1:
        print_message("Output verbosity level will be 0.", MessageType.SPEC)
    elif len(sys.argv) == 2:
        set_verbosity(sys.argv[1])
    else:
        print_message("Too much arguments - {}!".format(len(sys.argv)), MessageType.ERROR)
        exit(1)
    test_all()
