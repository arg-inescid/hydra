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


# Global variable
DEFAULT_PLOT_FILENAME = "tmp_plot_file.gplot"
FILENAME_LENGTH = 10
xlabel = {
    "startup": "Timelapse (ms)",
    "footprint": "Timelapse (ms)",
    "latency": "ApacheBench workload",
    "throughput": "ApacheBench workload",
    "scalability": "Timelapse (ms)"
}
ylabel = {
    "startup": "Startup time (ms)",
    "footprint": "Memory footprint (kbytes)",
    "latency": "99p latency (ms)",
    "throughput": "Throughput (req/sec)",
    "scalability": "Number of active lambdas"
}
plot_template = '''reset
set terminal pngcairo enhanced font "Verdana,64" size 4500,3000 linewidth 20
set output "{output}"
set grid
set key out top horizontal
set xlabel "{xlabel}"
set ylabel "{ylabel}"
set decimalsign locale
set yrange [0:]
set xrange [1:]
'''
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


def read_file_by_lines(filename):
    try:
        with open(filename) as input_file:
            return input_file.readlines()
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


def filter_file(filter_type, data):
    def sort_fun(x):
        res = x.split(' ')
        return int(res[0])

    if len(data) == 0:
        return None, None

    outputs = []
    titles = []
    regex = []
    regex_len = len(data['regex'])
    for index in range(regex_len):
        regex.append(re.compile(data['regex'][index]))
    for d in data:
        lines = read_file_by_lines(d['filepath'])
        if len(lines) == 0:
            continue
        index = 0
        tmp_list = []
        tmp = ""
        prev_line = ""
        for line in lines:
            match = regex[index].search(line)
            if match:
                index = (index + 1) % regex_len
                if regex_len == 1:
                    tmp_list.append(match.group(1))
                if regex_len == 2 and index == 0:
                    if filter_type == 'scalability':
                        tmp = match.group(1)
                        match = regex[index].search(prev_line)
                        if match:
                            tmp_list.append(str(match.group(1)) + ' ' + str(tmp))
                    else:
                        if match.group().find("Timestamp") == -1:
                            tmp_list.append(str(tmp) + ' ' + str(match.group(1)))
                        else:
                            tmp_list.append(str(match.group(1)) + ' ' + str(tmp))
                tmp = match.group(1)
            prev_line = line

        if regex_len == 2:
            tmp_list = sorted(tmp_list, key=sort_fun)
        outputs.append(tmp_list)
        titles.append(d['title'])
    return outputs, titles


def generate_tmp_files(results):
    letters = string.ascii_lowercase
    outputs = []
    for result in results:
        filename = ''.join(random.choice(letters) for _ in range(FILENAME_LENGTH)) + ".tmp"
        success = write_file(filename, '\n'.join(result))
        if success:
            outputs.append(filename)
    return outputs


def remove_tmp_files(files):
    for file in files:
        os.remove(file)


def combine_files(combine_info):
    for combine in combine_info:
        file_list = []
        for file in combine['input_files']:
            if file.find("*") > -1:
                file_dir = os.path.dirname(file)
                file_pattern = os.path.basename(file)[:-1]  # exclude asterisk
                for entry in sorted(os.listdir(os.path.dirname(file))):
                    if entry.find(file_pattern) > -1:
                        if entry.find("hotspot") > -1:
                            file_list.append(file_dir + "/../" + entry[:entry.find("_")] + "/shared/run.log")
                        file_list.append(os.path.join(file_dir, entry))
            else:
                file_list.append(file)
        write_file(combine['result_file'], '\n'.join([read_file(file) for file in file_list]))


# Core methods.
def run(command):
    outs, errs = subprocess.Popen(shlex.split(command),
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
    outs, errs = outs.decode(sys.stdout.encoding), errs.decode(sys.stdout.encoding)
    if len(errs) > 0:
        print_message("Command ({}) error log:\n{}".format(command, errs), MessageType.WARN)
    return outs


def format_plot_file(tmp_files, titles):
    command = '\n' + "plot "
    for index, data_file in enumerate(tmp_files):
        command += '"{data_file}" title "{title}" with lines'.format(data_file=data_file, title=titles[index])
        if index + 1 != len(tmp_files):
            command += ", \\" + '\n'
    return command


def generate_output(result, filter_type, output, titles):
    plot_file_data = plot_template.format(output=output, xlabel=xlabel[filter_type], ylabel=ylabel[filter_type])
    tmp_files = generate_tmp_files(result)
    write_file(DEFAULT_PLOT_FILENAME, plot_file_data + format_plot_file(tmp_files, titles))
    run("gnuplot {filename}".format(filename=DEFAULT_PLOT_FILENAME))
    tmp_files.append(DEFAULT_PLOT_FILENAME)
    remove_tmp_files(tmp_files)


def display_image(filename):
    run("eog {filename}".format(filename=filename))


def run_second_filter(results):
    tmp_lists = []
    for result in results:
        active_now = 0
        tmp_list = []
        for e in result:
            if e.find("Output") > 0:
                active_now += 1
                tmp_list.append(e.replace("Output", str(active_now)))
            else:
                active_now -= 1
                tmp_list.append(e.replace("Exit", str(active_now)))
        tmp_lists.append(tmp_list)
    return tmp_lists


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


def plot(plot_data):
    print_message("{} is running...".format(plot_data['plot']), MessageType.INFO)

    if 'combine_files' in plot_data:
        combine_files(plot_data['combine_files'])

    shown_images = []
    images = []
    for p in plot_data['plots']:
        result, titles = filter_file(p['type'], p['data'])
        if result:
            if p['type'] == 'scalability':
                result = run_second_filter(result)
            generate_output(result, p['type'], p['output'], titles)
            images.append((p['type'].upper(), p['output']))
            if p['show_image']:
                shown_image = threading.Thread(target=display_image, args=(p['output'],))
                shown_image.start()
        else:
            print_message("No data to be plotted for {}!".format(p['type']), MessageType.WARN)

    for shown_image in shown_images:
        shown_image.join()

    if 'start_server' in plot_data:
        generate_index_file(images, plot_data['start_server'])
        start_server(plot_data['start_server'])

    print_message("{} is running...done".format(plot_data['plot']), MessageType.INFO)


# Main function.
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print_message("Insufficient number of arguments ({})!".format(len(sys.argv)), MessageType.ERROR)
        exit(1)
    plot(load_data(sys.argv[1]))
