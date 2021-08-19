#!/usr/bin/python3
import enum
import sys

import run_install_deps
import run_plot
import run_test
import run_tests
import run_measurements


# Script type.
class ScriptType(enum.Enum):
    TEST = "test"
    PLOT = "plot"
    INSTALL_DEPS = "install-deps"
    TESTS = "tests"
    MEASUREMENTS = "measurements"


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
    if args[0] == ScriptType.MEASUREMENTS.value:
        run_measurements.main(args[1:])


# Main script for run command. Called from run.sh.
if __name__ == '__main__':
    parse_input_arguments(sys.argv[1:])
