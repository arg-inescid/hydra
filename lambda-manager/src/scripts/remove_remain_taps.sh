#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

# Acquire superuser privileges.
sudo ls >/dev/null

COMMON_TAP_NAME_PART="lmt-"

# Remove all taps with provided common tap name part.
mkfifo mypipe
ip a | grep "$COMMON_TAP_NAME_PART" | awk '{print $2}' | sed -n "/^$COMMON_TAP_NAME_PART.*$/p" | sed -e s/'://' >mypipe &
while IFS= read -r tap; do
  echo "$tap was deleted outside the pool."
  sudo bash "$DIR"/remove_taps.sh "$tap"
done <mypipe
rm mypipe
