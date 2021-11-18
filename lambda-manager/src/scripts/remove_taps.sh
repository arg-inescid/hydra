#!/bin/bash

# for i in {0..$#}; do
# 	i. argument - tap name
# done

# Remove taps.
arg_array=("$@")
for ((i = 0; i < $#; i++)); do
  ip link delete "${arg_array[$i]}" type tap
done
