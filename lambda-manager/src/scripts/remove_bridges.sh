#!/bin/bash

# for i in {0..$#};
# do
# 	i. argument - bridge name
# done

# Remove bridges.
arg_array=( "$@" )
for (( i=0; i<$#; i++ ));
do
  ip link set "${arg_array[$i]}" down
  brctl delbr "${arg_array[$i]}"
done
