#!/usr/bin/python

import os
import matplotlib.pyplot as plt
import numpy as np

results_home = "/home/rbruno/git/graalvm-argo-benchmarks/results"

benchmark_labels = [
    "java/hello-world",
    "python/hello-world",
    "javascript/hello-world",
    "java/sleep",
    "python/sleep",
    "javascript/sleep" ]

gv_benchmark_path = [
    "java/gv-hello-world-niuk",
    "python/gv-hello-world-niuk",
    "javascript/gv-hello-world-niuk",
    "java/gv-sleep-niuk",
    "python/gv-sleep-niuk",
    "javascript/gv-sleep-niuk" ]

cr_benchmark_path = [
    "java/cr-hello-world",
    "python/cr-hello-world",
    "javascript/cr-hello-world",
    "java/cr-sleep",
    "python/cr-sleep", 
    "javascript/cr-sleep" ]

gv_benchmark_avg_latency = {}
cr_benchmark_avg_latency = {}

def read_benchmark_latency(path, values):
    benchmark_latency = []
    with open(results_home + '/' + path + '/app.log') as file:
        for line in file:
            if 'Time taken:' in line:
                benchmark_latency.append(int(line.split()[2]))
    last_five_elements = benchmark_latency[-5:]
    values[path] = int(sum(last_five_elements) / len(last_five_elements))

for path in gv_benchmark_path: read_benchmark_latency(path, gv_benchmark_avg_latency)
for path in cr_benchmark_path: read_benchmark_latency(path, cr_benchmark_avg_latency)

x = np.arange(len(benchmark_labels))
width = 0.25

fig, ax = plt.subplots()
ax.bar(x - width/2, gv_benchmark_avg_latency.values(), width, label='Graalvisor')
ax.bar(x + width/2, cr_benchmark_avg_latency.values(), width, label='OpenWisk')

ax.set_ylabel('Time (us)')
ax.set_xticks(x, benchmark_labels)
ax.legend()
plt.show()
