#!/usr/bin/env python3
from matplotlib import pyplot as plt
import numpy as np

modes = ['kn', 'ow', 'gh']
conc_values = ['1', '4',  '16', '64']
benchmarks = ['hw', 'fh', 'up']

def load_lat(fpath, pattern):
  with open(fpath) as f:
    for line in f:
      if pattern not in line:
        continue
      return int(line.strip().split()[1])

def load_tput(fpath):
  with open(fpath) as f:
    for line in f:
      #Requests per second:    221.79 [#/sec] (mean)
      if "Requests per second:" not in line:
        continue
      return float(line.strip().split()[3])

def plot_latency(benchmark, benchmark_name, percentile):
  # Dict of of dict with the values:
  # values['kn']['fh'] -> [values]
  values = {}

  # Set font size.
  plt.rcParams.update({'font.size': 8})

  for base in modes:
    values[base] = {}
    values[base][benchmark] = []
    for conc in conc_values:
      # user-kn_jv_fh-32.log
      values[base][benchmark].append(load_lat("results/user-{}_jv_{}-{}.log".format(base, benchmark, conc), "{}%".format(percentile)))

  fig, ax = plt.subplots()
  offset = 0
  x = np.arange(len(conc_values))
  for base in modes:
    ax.bar(x + offset, values[base][benchmark], width=.25, label=base)
    ax.set_xticks(x, conc_values)
    offset = offset + .25

  ax.legend(loc='upper left')
  ax.set_axisbelow(True)
  plt.xticks(rotation=45)
  plt.ylabel('{} {} latency (ms)'.format(benchmark_name, percentile))
  plt.grid(axis='y')
  plt.tight_layout()
  plt.savefig('results/ab-{}-p{}.pdf'.format(benchmark, percentile))
  plt.savefig('results/ab-{}-p{}.png'.format(benchmark, percentile), dpi=300)

def plot_throughput(benchmark, benchmark_name):
  # Dict of of dict with the values:
  # values['kn']['fh'] -> [values]
  values = {}

  # Set font size.
  plt.rcParams.update({'font.size': 8})

  for base in modes:
    values[base] = {}
    values[base][benchmark] = []
    for conc in conc_values:
      # user-kn_jv_fh-32.log
      values[base][benchmark].append(load_tput("results/user-{}_jv_{}-{}.log".format(base, benchmark, conc)))

  fig, ax = plt.subplots()
  offset = 0
  x = np.arange(len(conc_values))
  for base in modes:
    ax.bar(x + offset, values[base][benchmark], width=.25, label=base)
    ax.set_xticks(x, conc_values)
    offset = offset + .25

  ax.legend(loc='upper left')
  ax.set_axisbelow(True)
  plt.xticks(rotation=45)
  plt.ylabel('{} Throughput (ops/s)'.format(benchmark_name))
  plt.grid(axis='y')
  plt.tight_layout()
  plt.savefig('results/ab-{}-tput.pdf'.format(benchmark))
  plt.savefig('results/ab-{}-tput.png'.format(benchmark), dpi=300)

plot_latency('hw', 'Hello World', '50')
plot_latency('hw', 'Hello World', '90')
plot_latency('hw', 'Hello World', '99')
plot_latency('fh', 'File Hashing', '50')
plot_latency('fh', 'File Hashing', '90')
plot_latency('fh', 'File Hashing', '99')
plot_latency('hr', 'HTTP Request', '50')
plot_latency('hr', 'HTTP Request', '90')
plot_latency('hr', 'HTTP Request', '99')
plot_throughput('hw', 'Hello World')
plot_throughput('fh', 'File Hashing')
plot_throughput('hr', 'HTTP Request')