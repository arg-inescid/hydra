#!/bin/bash

# for i in {0..$#}; do
#   i. argument   - tap name
#   i+1. argument - lambda ip address
# done

# Create taps.
arg_array=("$@")
for ((i = 0; i < $#; i += 2)); do
  ip tuntap add "${arg_array[$i]}" mode tap user root
  ip link set "${arg_array[$i]}" up
  ip route add "${arg_array[$((i + 1))]}" dev "${arg_array[$i]}"
done
