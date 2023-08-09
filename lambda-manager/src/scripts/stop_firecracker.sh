#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"


LAMBDA_HOME=$1
if [ -z "$LAMBDA_HOME" ]; then
  echo "Lambda home is not present."
  exit 1
fi

LAMBDA_NAME=$2
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

LAMBDA_MODE=$3
if [ -z "$LAMBDA_MODE" ]; then
  echo "Lambda mode is not present."
  exit 1
fi

LAMBDA_IP=$4
if [ -z "$LAMBDA_IP" ]; then
  echo "Lambda ip is not present."
  exit 1
fi

LAMBDA_PORT=$5
if [ -z "$LAMBDA_PORT" ]; then
  echo "Lambda port is not present."
  exit 1
fi

LAMBDA_ID=$6

function stop_firecracker {
  LAMBDA_PID=$(cat "$LAMBDA_HOME"/lambda.pid)
  sudo kill $LAMBDA_PID
  # Sleep to ensure that devmapper resources are released.
  sleep 1
  # Cleanup overlay.
  sudo bash "$DIR"/devmapper/delete_overlay_image.sh "$LAMBDA_NAME" "$LAMBDA_HOME"/*.img
}

function stop_firecracker_snapshot {
  FIRECRACKER_ID=lambda"$LAMBDA_ID"id
  LAMBDA_PID=$(ps aux | grep $FIRECRACKER_ID | grep firecracker | awk '{print $2}')
  sudo kill $LAMBDA_PID
  # Sleep to ensure that devmapper resources are released.
  sleep 1
  # Delete namespace.
  sudo ip netns delete ns$LAMBDA_ID
  # Cleanup overlay.
  sudo umount "$LAMBDA_HOME"/chroot/root/*.img
  sudo umount "$LAMBDA_HOME"/chroot/root/hello-vmlinux.bin
  sudo umount "$LAMBDA_HOME"/chroot/root/snapshot_file
  sudo umount "$LAMBDA_HOME"/chroot/root/mem_file
  sudo bash "$DIR"/devmapper/delete_overlay_image.sh "$LAMBDA_NAME" "$LAMBDA_HOME"/chroot/root/*.img
  # Remove other unused files.
  rm -rf "$LAMBDA_HOME"/chroot
}

if [ "$LAMBDA_MODE" == "HOTSPOT_W_AGENT" ]; then
  # Collect configuration.
  mkdir "$LAMBDA_HOME"/config
  for config_name in jni predefined-classes proxy reflect resource serialization; do
    curl -s -X POST "$LAMBDA_IP":"$LAMBDA_PORT"/agentconfig -H 'Content-Type: application/json' --data "{\"configName\":\"$config_name\"}" -o "$LAMBDA_HOME"/config/"$config_name"-config.json
  done
fi

if [[ -z "$LAMBDA_ID" ]]; then
  # Lambda was created normally.
  stop_firecracker
else
  # Lambda ID was provided -> lambda was created from snapshot.
  stop_firecracker_snapshot
fi
