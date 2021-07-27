#!/usr/bin/python3
import enum
import glob
import json
import os
import shlex
import subprocess
import sys
from json import JSONDecodeError


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO]"
    ERROR = "[ERR0]"
    WARN = "[WARN]"
    SPEC = "[SPEC]"
    NO_HEADER = ""


# Global variables.
MAX_VERBOSITY_LVL = 2
VERBOSITY_LVL = 0

SETUP_DB_VM_LOC = os.path.join("..", "lambda-manager", "src", "scripts", "qemu-jvm")
SETUP_DB_VM_FILE = "setup_debian_vm.sh"
SETUP_SCRIPT = '''#!/usr/bin/bash
cd {location}
bash {file}
'''

MASK = "24"
GET_DEF_GATEWAY_FILE = "get_default_gateway.sh"
GET_DEF_GATEWAY_SCRIPT = '''
#!/usr/bin/bash
ip r | grep default | awk '{print $3}' | head -n 1
'''


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


def install_required_packages():
    print_message("Installing required packages...", MessageType.INFO)
    run(sys.executable + " -m pip install requests")
    print_message("Installing required packages...done", MessageType.INFO)


# File utils.
def read_json_file(filename):
    try:
        with open(filename) as input_file:
            return json.load(input_file)
    except IOError:
        print_message("Input file {} is missing or deleted!".format(filename), MessageType.ERROR)
        exit(1)
    except JSONDecodeError as err:
        print_message("Bad JSON syntax: {}".format(err), MessageType.ERROR)
        exit(1)


def write_json_file(filename, content):
    with open(filename, 'w') as outfile:
        json.dump(content, outfile)


def write_file(filename, content):
    try:
        with open(filename, 'w') as output_file:
            output_file.write(content)
    except IOError:
        print_message("Error during writing in output file {}!".format(filename), MessageType.ERROR)
        exit(1)


def remove_file(filename):
    os.remove(filename)


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


def make_kernel_writable():
    print_message("Making kernel writable...", MessageType.INFO)
    kernel_location = os.path.join("/", "boot")
    kernel_files = sorted(glob.glob(os.path.join(kernel_location, "vmlinuz-*-generic")))
    if len(kernel_files) == 0:
        print_message("Something went wrong! No kernel files found!", MessageType.ERROR)
        exit(1)
    run("sudo chmod +w {}".format(os.path.join(kernel_location, kernel_files[-1])))
    print_message("Making kernel writable...done", MessageType.INFO)


def install_world():
    print_message("Installing world...", MessageType.INFO)
    run("sudo apt-get update")
    # Installing GCC and system utils...
    run("sudo apt-get install -y gcc libz-dev libguestfs-tools bridge-utils")
    # Installing qemu...
    run("sudo apt-get install -y qemu qemu-kvm")
    # Installing java...
    run("sudo apt-get install -y default-jdk default-jre")
    # Installing load balancer (nginx)...
    run("sudo apt-get install -y nginx")
    # Installing build tools...
    run("sudo apt-get install -y maven")
    # Installing tools for testing and plotting...
    run("sudo apt-get install -y apache2-utils gnuplot")
    print_message("Installing the world...done", MessageType.INFO)


def download_resources():
    print_message("Downloading resources...", MessageType.INFO)
    print_message("Downloading resources...done", MessageType.INFO)


def setup_debian_vm():
    print_message("Setup debian vm...", MessageType.INFO)
    write_file(SETUP_DB_VM_FILE, SETUP_SCRIPT.format(location=SETUP_DB_VM_LOC, file=SETUP_DB_VM_FILE))
    run("bash {script}".format(script=SETUP_DB_VM_FILE))
    remove_file(SETUP_DB_VM_FILE)
    print_message("Setup debian vm...done", MessageType.INFO)


def setup_bridge():
    print_message("Creating bridge...", MessageType.INFO)

    # Get default gateway.
    write_file(GET_DEF_GATEWAY_FILE, GET_DEF_GATEWAY_SCRIPT)
    default_gateway = run("bash {file}".format(file=GET_DEF_GATEWAY_FILE))
    remove_file(GET_DEF_GATEWAY_FILE)

    # Replace existing gateway address in configs/manager/default-manager.json.
    manager_config_path = os.path.join("configs", "manager", "default-manager.json")
    manager_config = read_json_file(manager_config_path)
    manager_config['gateway'] = "{}/{}".format(default_gateway[:-1], MASK)
    write_json_file(manager_config_path, manager_config)

    print_message("Creating bridge...done", MessageType.INFO)


# Main function.
def main(args):
    if len(args) == 1:
        set_verbosity(args[0])
    else:
        print_message("Output verbosity level will be 0.", MessageType.SPEC)
    install_required_packages()
    make_kernel_writable()
    install_world()
    download_resources()
    setup_debian_vm()
    setup_bridge()


if __name__ == '__main__':
    main(sys.argv[1:])
