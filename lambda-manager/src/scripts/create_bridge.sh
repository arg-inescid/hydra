#!/bin/bash

bridge=lmb
gateway=172.18.0.1
smask=16

# Create bridge if not already created
if [ ! -d "/sys/class/net/$bridge" ]; then
    defaultdevice=$(ip route get 8.8.8.8 | grep -Po '(?<=(dev ))(\S+)')
    sudo ip link add name $bridge type bridge
    sudo ip addr add $gateway/$smask brd + dev $bridge
    sudo ip link set dev $bridge up
    sudo iptables -A FORWARD -o $bridge -j ACCEPT
    sudo iptables -A FORWARD -i $bridge -j ACCEPT
    sudo iptables -t nat -A POSTROUTING -o $defaultdevice -j MASQUERADE
    sudo iptables -A FORWARD -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
fi
