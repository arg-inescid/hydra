#!/usr/bin/python3

import matplotlib.pyplot as plt
import numpy as np

hotspot_latency = []
hotspot_latency_p = []
vmm_latency = []
vmm_latency_p = []


def requiest_latency_cdf():
    with open('../lambda-manager/src/manager_logs/lambda_manager.log') as f:
        lines = f.read().splitlines()

        # Select lines with vmm_id
        vmm_latency = [line for line in lines if "vmm_id" in line]
        hotspot_latency = [line for line in lines if "hotspot_id" in line]

        # Select the 6th collumn
        vmm_latency = [line.split()[5] for line in vmm_latency]
        hotspot_latency = [line.split()[5] for line in hotspot_latency]

        # Create numpy array from list
        vmm_latency = np.asarray(vmm_latency, dtype=np.int32)
        hotspot_latency = np.asarray(hotspot_latency, dtype=np.int32)

        # Sort the array
        vmm_latency.sort()
        hotspot_latency.sort()

        # Calculate the x values
        vmm_latency_p = 1. * np.arange(len(vmm_latency)) / (len(vmm_latency) - 1)
        hotspot_latency_p = 1. * np.arange(len(hotspot_latency)) / (len(hotspot_latency) - 1)

    fig = plt.figure()
    ax = fig.add_subplot(111)
    ax.plot(hotspot_latency, hotspot_latency_p, label="HotSpot")
    ax.plot(vmm_latency, vmm_latency_p, label="VMM")
    ax.set_xlabel('Request Latency (ms)')
    ax.set_xscale('log')
    ax.legend()
    fig.savefig("request_latency_cdf.png")

requiest_latency_cdf()
