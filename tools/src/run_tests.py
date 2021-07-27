#!/usr/bin/python3
import enum
import os
import sys
import run_test


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO]"
    ERROR = "[ERR0]"
    WARN = "[WARN]"
    SPEC = "[SPEC]"
    NO_HEADER = ""


# Test global variables.
TESTING_DIR = os.path.join("configs", "tests")
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
            print_message("Test - {} - is running...".format(file.split(".json")[0]), MessageType.SPEC)
            run_test.main(["v" * VERBOSITY_LVL, os.path.join(root, file)])
            print_message("Test - {} - is running...done".format(file.split(".json")[0]), MessageType.SPEC)
        print_message("{}...done".format(current_tier), MessageType.INFO)


# Main function.
def main(args):
    if len(args) == 0:
        print_message("Output verbosity level will be 0.", MessageType.SPEC)
    elif len(args) == 1:
        set_verbosity(args[0])
    else:
        print_message("Too much arguments - {}!".format(len(args)), MessageType.ERROR)
        exit(1)
    test_all()


if __name__ == '__main__':
    main(sys.argv[1:])
