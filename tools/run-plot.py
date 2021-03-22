#!/usr/bin/python3
import enum
import json
import os
import random
import re
import string
import subprocess
import sys
from json import JSONDecodeError
from time import sleep

from PIL import Image
from pygnuplot import gnuplot


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO]"
    ERROR = "[ERR0]"
    WARN = "[WARN]"
    SPEC = "[SPEC]"


# Global variable
filter_mapping = {
    "latency": "Time taken for tests",
    "transfer-rate": "Requests per second",
    "memory": "Nothing for now."
}
label_mapping = {
    "latency": "Time consumption (ms)",
    "transfer-rate": "Requests per second (#/sec)",
    "memory": "Nothing for now."
}
title_mapping = {
    "latency": "Time consumption",
    "transfer-rate": "Requests per second",
    "memory": "Nothing for now."
}


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
    required_packages = subprocess.check_output([sys.executable, '-m', 'pip', 'freeze'])
    installed_packages = [r.decode().split('==')[0] for r in required_packages.split()]
    print_message("Installed packages: [ " + " ".join(installed_packages) + " ]", MessageType.WARN)


# File util methods.
def load_data(filename):
    try:
        with open(filename) as input_file:
            return json.load(input_file)
    except IOError:
        print_message("Input file {} is missing or deleted!".format(filename), MessageType.ERROR)
        exit(1)
    except JSONDecodeError as err:
        print_message("Bad JSON syntax: {}".format(err), MessageType.ERROR)
        exit(1)


def read_file(filename):
    try:
        with open(filename) as input_file:
            return input_file.read()
    except IOError:
        print_message("Input file {} is missing or deleted!".format(filename), MessageType.WARN)
    return ""


def write_file(filename, output):
    try:
        with open(filename, 'w') as output_file:
            output_file.write(output)
        return True
    except IOError:
        print_message("Error during writing in output file {}!".format(filename), MessageType.WARN)
        return False


def filter_file(filter_type, input_files):
    if len(input_files) == 0:
        print_message("No data to be plotted for {}!".format(filter_type), MessageType.WARN)
        return None
    outputs = []
    titles = []
    regex = re.compile("{}: *([0-9.]*)".format(filter_mapping[filter_type]))
    for input_file in input_files:  # 0: plot title, 1: plot data
        if len(input_file) < 2:
            print_message("Insufficient number of arguments for plot command ({})!".format(len(input_file)),
                          MessageType.WARN)
            continue
        file = read_file(input_file[1])
        if len(file) == 0:
            continue
        outputs.append(regex.findall(file))
        titles.append(input_file[0])
    return outputs, titles


def generate_tmp_files(results):
    letters = string.ascii_lowercase
    outputs = []
    for result in results:
        filename = ''.join(random.choice(letters) for _ in range(10)) + ".tmp"
        success = write_file(filename, '\n'.join(result))
        if success:
            outputs.append(filename)
    return outputs


def remove_tmp_files(files):
    for file in files:
        os.remove(file)


# Core methods.
def generate_command(data_files, titles):
    command = ""
    for index, data_file in enumerate(data_files):
        command += '"{data_file}" title "{title}" with lines'.format(data_file=data_file, title=titles[index])
        if index + 1 != len(data_files):
            command += ", \\ " + '\n'
    return command


def generate_output(result, filter_type, output, titles):
    g = gnuplot.Gnuplot()
    g.cmd('''
            reset
            set terminal pngcairo enhanced font "Verdana,64" size 4500,3000 linewidth 20
            set output "{output}"
            set grid
            set key out top horizontal
            set xlabel "Iteration"
            set ylabel "{ylabel}"
            set decimalsign locale
            set yrange [0:]
          '''.format(output=output, ylabel=label_mapping[filter_type]))
    tmp_files = generate_tmp_files(result)
    g.plot(generate_command(tmp_files, titles))
    sleep(10)   # Wait until plot is finished.
    remove_tmp_files(tmp_files)


def display_image(filename):
    Image.open(filename).show()


def plot(data):
    print_message("{} is running...".format(data['plot']), MessageType.INFO)

    for p in data['plots']:
        result, titles = filter_file(p['type'], p['data'])
        if result:
            generate_output(result, p['type'], p['output'], titles)
            if p['show_image']:
                display_image(p['output'])

    print_message("{} is running...done".format(data['plot']), MessageType.INFO)


# Main function.
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print_message("Insufficient number of arguments ({})!".format(len(sys.argv)), MessageType.ERROR)
        exit(1)
    install_required_packages()
    plot(load_data(sys.argv[1]))
