#!/bin/bash

# for i in {0..$#};
# do
# 	i. argument - bridge name
#	  i+1. argument - bridge address space
#	  i+2. argument - bridge address
# done

# Setup bridges.
arg_array=( "$@" )
for (( i=0; i<$#; i+=3 ));
do
	brctl addbr "${arg_array[$i]}"
	ip addr add "${arg_array[$((i+2))]}" dev "${arg_array[$i]}"
	ip link set "${arg_array[$i]}" up
	ip route add "${arg_array[$((i+1))]}" dev "${arg_array[$i]}"
done