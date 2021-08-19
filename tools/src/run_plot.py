#!/usr/bin/python3
import base64
import enum
import http.server
import json
import os
import random
import re
import shlex
import socketserver
import string
import subprocess
import sys
import threading
from json import JSONDecodeError


# Message type.
class MessageType(enum.Enum):
    INFO = "[INFO]"
    ERROR = "[ERR0]"
    WARN = "[WARN]"
    SPEC = "[SPEC]"
    NO_HEADER = ""


# Plot type.
class PlotType(enum.Enum):
    LATENCY = "latency"
    THROUGHPUT = "throughput"
    STARTUP = "startup"
    FOOTPRINT = "footprint"
    SCALABILITY = "scalability"
    TOTAL_MEMORY = "total memory"


# Plot global variables.
MAX_VERBOSITY_LVL = 2
VERBOSITY_LVL = 0

lambda_logs = {}
manager_log = []

LAMBDA_LOGS_DIR = os.path.join("..", "..", "lambda-manager", "lambda_logs")
MANAGER_LOG_FILE = os.path.join("..", "..", "lambda-manager", "manager_logs", "lambda_manager.log")
MANAGER_LOG_REGEX = "Timestamp \\((\\d+)\\).*PID -> (\\d+).*(Output|Exit)"
PLOT_FILENAME = "plot_file.gplot"
FILENAME_LENGTH = 10
xlabel = {
    "latency": "ApacheBench workload",
    "throughput": "ApacheBench workload",
    "startup": "Timelapse (ms)",
    "footprint": "Timelapse (ms)",
    "scalability": "Timelapse (ms)",
    "total memory": "Timelapse (ms)"
}
ylabel = {
    "latency": "99p latency (ms)",
    "throughput": "Throughput (req/sec)",
    "startup": "Startup time (ms)",
    "footprint": "Memory footprint (MB)",
    "scalability": "Number of active lambdas",
    "total memory": "Total system memory (GB)"
}
plot_template = '''reset
set terminal pngcairo enhanced font "Verdana,64" size 4500,3000 linewidth 20
set output "{output}"
set grid
set key out top horizontal
set xlabel "{xlabel}"
set ylabel "{ylabel}"
set decimalsign '.'
set yrange [0:]
set xrange [1:]
'''

# HTML report global variables.
html_header = '''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Benchmarks</title>
</head>
<body>
    <h1>RESULTS</h1>
'''
html_footer = '''</body>
</html>
'''
html_title = "<h3>{}</h3>"
html_image = "<img src='data:image/png;base64,{src}' width='{w}' height='{h}'/>"


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


def kb_to_mb(value):
    return value / 1000


def kb_to_gb(value):
    return value / 1_000_000


def set_verbosity(flag):
    global VERBOSITY_LVL
    v_count = flag.count("v")
    if v_count != len(flag):
        print_message("Verbosity flag should be v or vv instead of {flag}. Output verbosity level will fallback to 0."
                      .format(flag=flag), MessageType.WARN)
        return
    VERBOSITY_LVL = min(v_count, MAX_VERBOSITY_LVL)
    print_message("Output verbosity level is set to {level}.".format(level=VERBOSITY_LVL),
                  MessageType.SPEC)


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
        print_message("Input file {} is missing or deleted!".format(filename), MessageType.ERROR)
        exit(1)


def write_file(filename, content):
    try:
        with open(filename, 'w') as output_file:
            output_file.write(content)
    except IOError:
        print_message("Error during writing in output file {}!".format(filename), MessageType.ERROR)
        exit(1)


def filterApacheBenchFile(plot_config_dir, plots_data):
    outputs = []
    titles = []
    for plot_data in plots_data:
        file = read_file(os.path.join(plot_config_dir, plot_data['filepath']))
        if len(file) == 0:
            continue
        regex = re.compile(plot_data['regex'])
        outputs.append(regex.findall(file))
        titles.append(plot_data['title'])
    return outputs, titles


def generate_hm_entry(root, filename):
    filename, extension = os.path.splitext(filename)
    return filename + "_" + os.path.basename(root).split("_")[1] + extension


def readAllLambdaLogFiles():
    if len(lambda_logs) > 0:
        return
    for root, dirs, files in os.walk(LAMBDA_LOGS_DIR):
        for filename in files:
            if filename == ".gitkeep":
                continue
            lambda_logs[generate_hm_entry(root, filename)] = read_file(os.path.join(root, filename))


def readManagerLogFile():
    global manager_log
    if len(manager_log) > 0:
        return
    file = read_file(MANAGER_LOG_FILE)
    regex = re.compile(MANAGER_LOG_REGEX)
    manager_log = regex.findall(file)


def get_key(plot_type, identifier):
    if plot_type == PlotType.FOOTPRINT.value or plot_type == PlotType.SCALABILITY.value \
            or plot_type == PlotType.TOTAL_MEMORY.value:
        return "memory_{}.log".format(identifier)
    if plot_type == PlotType.STARTUP.value:
        return "output_{}.log".format(identifier)


def footprint_startup(plot_type, plot_data):
    output = []
    regex = re.compile(plot_data['regex'])
    for entry in manager_log:
        key = get_key(plot_type, entry[1])
        if key in lambda_logs and entry[2] != "Exit":
            find_results = regex.findall(lambda_logs[key])
            if len(find_results) > 0:
                value = int(find_results[0])
                if plot_type == PlotType.FOOTPRINT.value:
                    value = kb_to_mb(value)
                output.append("{} {}".format(entry[0], value))
            else:
                print_message("Regex and content are not matching! Regex: {}. Content: {}".format(
                    plot_data['regex'], lambda_logs[key]), MessageType.WARN)
    return [output], [plot_data['title']]


def scalability_total_memory(plot_type, plot_data):
    output = []
    column = 0
    if plot_type == PlotType.TOTAL_MEMORY.value:
        regex = re.compile(plot_data['regex'])
    for entry in manager_log:
        key = get_key(plot_type, entry[1])
        if key in lambda_logs:
            if entry[2] == "Output":
                if plot_type == PlotType.SCALABILITY.value:
                    column += 1
                else:
                    find_results = regex.findall(lambda_logs[key])
                    if len(find_results) > 0:
                        column += kb_to_gb(int(find_results[0]))
                    else:
                        print_message("Regex and content are not matching! Regex: {}. Content: {}".format(
                            plot_data['regex'], lambda_logs[key]), MessageType.WARN)
            else:
                if plot_type == PlotType.SCALABILITY.value:
                    column -= 1
                else:
                    find_results = regex.findall(lambda_logs[key])
                    if len(find_results) > 0:
                        column -= kb_to_gb(int(find_results[0]))
                    else:
                        print_message("Regex and content are not matching! Regex: {}. Content: {}".format(
                            plot_data['regex'], lambda_logs[key]), MessageType.WARN)
            output.append("{} {}".format(entry[0], column))
    return [output], [plot_data['title']]


def filterLambdaLogFiles(plot_type, plot_data):
    readManagerLogFile()
    readAllLambdaLogFiles()
    if plot_type == PlotType.FOOTPRINT.value or plot_type == PlotType.STARTUP.value:
        return footprint_startup(plot_type, plot_data)
    if plot_type == PlotType.SCALABILITY.value or plot_type == PlotType.TOTAL_MEMORY.value:
        return scalability_total_memory(plot_type, plot_data)


def filter_file(plot_config_dir, plot_type, plots_data):
    if plot_type == PlotType.LATENCY.value or plot_type == PlotType.THROUGHPUT.value:
        return filterApacheBenchFile(plot_config_dir, plots_data)
    else:
        return filterLambdaLogFiles(plot_type, plots_data[0])


def generate_tmp_files(plot_data):
    letters = string.ascii_lowercase
    outputs = []
    for result in plot_data:
        filename = ''.join(random.choice(letters) for _ in range(FILENAME_LENGTH)) + ".tmp"
        write_file(filename, '\n'.join(result))
        outputs.append(filename)
    return outputs


def remove_tmp_files(files):
    for file in files:
        os.remove(file)


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


def format_plot_file(tmp_data_files, plot_titles):
    command = '\n' + "plot "
    for index, data_file in enumerate(tmp_data_files):
        command += '"{data_file}" title "{title}" with lines'.format(data_file=data_file, title=plot_titles[index])
        if index + 1 != len(tmp_data_files):
            command += ", \\" + '\n'
    return command


def generate_output(plot_data, plot_titles, filter_type, output):
    plot_file = plot_template.format(output=output, xlabel=xlabel[filter_type], ylabel=ylabel[filter_type])
    tmp_data_files = generate_tmp_files(plot_data)
    write_file(PLOT_FILENAME, plot_file + format_plot_file(tmp_data_files, plot_titles))
    run("gnuplot {filename}".format(filename=PLOT_FILENAME))
    tmp_data_files.append(PLOT_FILENAME)
    remove_tmp_files(tmp_data_files)


def display_image(filename):
    run("eog {filename}".format(filename=filename))


def generate_index_file(images, server_info):
    content = html_header
    for image in images:
        content += '\t' + html_title.format(image[0]) + '\n' + '\t' \
                   + html_image.format(src=base64.b64encode(open(image[1], 'rb').read()).decode('ascii'),
                                       w=server_info['img_width'],
                                       h=server_info['img_height']) \
                   + '\n '
    content += html_footer
    write_file("index.html", content)


def start_server(server_info):
    try:
        with socketserver.TCPServer(("", server_info['port']), http.server.SimpleHTTPRequestHandler) as httpd:
            print_message("Server is listening on {}".format(server_info['port']), MessageType.INFO)
            httpd.serve_forever()
    except KeyboardInterrupt:
        remove_tmp_files(['index.html'])


def plot(plot_config_dir, plots_info):
    print_message("{} is running...".format(plots_info['plot']), MessageType.INFO)

    images = []
    displayed_images = []
    for plot_info in plots_info['plots']:
        plot_data, plot_titles = filter_file(plot_config_dir, plot_info['type'], plot_info['data'])
        if plot_data and plot_titles:
            real_output_path = os.path.join(plot_config_dir, plot_info['output'])
            generate_output(plot_data, plot_titles, plot_info['type'], real_output_path)
            images.append((plot_info['type'].upper(), real_output_path))
            if plot_info['show_image']:
                displayed_image = threading.Thread(target=display_image, args=(real_output_path,))
                displayed_images.append(displayed_image)
                displayed_image.start()
        else:
            print_message("No data to be plotted for {}!".format(plot_info['type']), MessageType.WARN)

    for displayed_image in displayed_images:
        displayed_image.join()

    if 'start_server' in plots_info:
        generate_index_file(images, plots_info['start_server'])
        start_server(plots_info['start_server'])

    print_message("{} is running...done".format(plots_info['plot']), MessageType.INFO)


# Main function.
def main(args):
    global LAMBDA_LOGS_DIR, MANAGER_LOG_FILE

    LAMBDA_LOGS_DIR = os.path.join(os.path.dirname(sys.argv[0]), LAMBDA_LOGS_DIR)
    MANAGER_LOG_FILE = os.path.join(os.path.dirname(sys.argv[0]), MANAGER_LOG_FILE)

    if len(args) == 0:
        print_message("Insufficient number of arguments - {}!".format(len(args)), MessageType.ERROR)
        exit(1)
    plot_config_index = 0
    if len(args) == 1:
        plot_config_index = 0
        print_message("Output verbosity level will be 0.", MessageType.SPEC)
    elif len(args) == 2:
        plot_config_index = 1
        set_verbosity(args[0])
    else:
        print_message("Too much arguments - {}!".format(len(args)), MessageType.ERROR)
        exit(1)
    plot(os.path.dirname(args[plot_config_index]), load_data(args[plot_config_index]))


if __name__ == '__main__':
    main(sys.argv[1:])
