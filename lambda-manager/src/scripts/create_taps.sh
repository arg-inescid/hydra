#!/bin/bash

# for i in {0..$#};
# do
# 	i. argument - bridge name
#	  i+1. argument - tap name
# done

# Create taps.
arg_array=( "$@" )
for (( i=0; i<$#; i+=2 ));
do
  ip tuntap add "${arg_array[$((i+1))]}" mode tap user root
  ip link set dev "${arg_array[$((i+1))]}" master "${arg_array[$i]}"
  ip link set "${arg_array[$((i+1))]}" up
done
