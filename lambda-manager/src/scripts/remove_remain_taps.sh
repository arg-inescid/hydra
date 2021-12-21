#!/bin/bash

# Acquire superuser privileges.
sudo ls >/dev/null

COMMON_TAP_NAME_PART="lmt-"

# Kill orphan qemu processes.
sudo pkill qemu

# Remove all taps with provided common tap name part.
mkfifo mypipe
ip a | grep "$COMMON_TAP_NAME_PART" | awk '{print $2}' | sed -n "/^$COMMON_TAP_NAME_PART.*$/p" | sed -e s/'://' >mypipe &
while IFS= read -r tap; do
  echo "$tap was deleted outside the pool."
  sudo bash src/scripts/remove_taps.sh "$tap"
done <mypipe
rm mypipe
