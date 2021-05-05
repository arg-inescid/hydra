import enum
import glob
import os
import shlex
import subprocess
import sys

import requests


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO]"
    ERROR = "[ERR0]"
    WARN = "[WARN]"
    SPEC = "[SPEC]"


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


def install_required_packages():
    print_message(run(sys.executable + " -m pip install requests"), MessageType.WARN)


# Core methods.
def run(command):
    outs, errs = subprocess.Popen(shlex.split(command),
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
    outs, errs = outs.decode(sys.stdout.encoding), errs.decode(sys.stdout.encoding)
    if len(errs) > 0:
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
    run("sudo apt-get install gcc libz-dev libguestfs-tools qemu qemu-kvm maven apache2-utils gnuplot")
    print_message("Installing the world...done", MessageType.INFO)


def install_nginx():
    print_message("Installing nginx...", MessageType.INFO)
    run("sudo apt-get install nginx")
    print_message("Installing the nginx...done", MessageType.INFO)


def install_docker():
    print_message("Installing docker...", MessageType.INFO)  # https://docs.docker.com/engine/install/ubuntu/
    run("sudo apt-get update")
    run("sudo apt-get install apt-transport-https ca-certificates gnupg-agent software-properties-common curl wget")
    run("curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -")
    run("sudo apt-key fingerprint 0EBFCD88")
    run("sudo add-apt-repository 'deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable'")
    run("sudo apt-get update")
    run("sudo apt-get install docker-ce docker-ce-cli containerd.io")
    print_message("Installing docker...done", MessageType.INFO)


def install_native_image():
    pass


def setup_virtualization():
    pass


# Main function.
if __name__ == '__main__':
    install_required_packages()
    make_kernel_writable()
    install_world()
    # install_nginx()
    # install_docker()
    # install_native_image()
    # setup_virtualization()
