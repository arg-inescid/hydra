#!/bin/bash
#   1. argument - Common gateway IP address part.

# Acquire superuser privileges.
sudo ls >/dev/null

COMMON_GATEWAY_PART=$1
if [ -z "$COMMON_GATEWAY_PART" ]; then
  echo "Common gateway IP address part is not provided!"
  exit 1
fi

# Kill orphan qemu processes.
sudo pkill qemu

# Remove all taps with provided common gateway part.
mkfifo mypipe
ip r | grep "$COMMON_GATEWAY_PART" | grep linkdown | awk '{print $3}' >mypipe &
while IFS= read -r tap; do
  echo "Deleting tap $tap..."
  sudo bash src/scripts/remove_taps.sh "$tap"
done <mypipe
rm mypipe
