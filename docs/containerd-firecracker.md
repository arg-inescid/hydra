# Setting up containerd-firecracker


To support custom runtimes we rely on [containerd-firecracker](https://github.com/firecracker-microvm/firecracker-containerd) which allow enables containerd to deploy Firecracker microVMs instead of regular containers.

This tutorial is inspited on the [getting-started.md](https://github.com/firecracker-microvm/firecracker-containerd/blob/main/docs/getting-started.md) available for containerd-firecracker. The goal is this tutorial summaryze the steps to get containerd-firecracker working. If you want additional detail or experience any problems, please resort to [getting-started.md](https://github.com/firecracker-microvm/firecracker-containerd/blob/main/docs/getting-started.md).

#### Step 1: clone [containerd-firecracker](https://github.com/firecracker-microvm/firecracker-containerd)

`git clone --recurse-submodules git@github.com:firecracker-microvm/firecracker-containerd.git`

#### Step 2: build tools

```
make all
make firecracker
make image
sudo make demo-network
```

#### Step 3: download linux

`curl -fsSL -o demo/hello-vmlinux.bin https://s3.amazonaws.com/spec.ccfc.min/img/quickstart_guide/x86_64/kernels/vmlinux.bin`

#### Step 4: setup environment

```
sudo mkdir -p /var/run/firecracker-containerd/runtime
sudo mkdir -p /var/lib/firecracker-containerd/runtime
sudo mkdir -p /var/lib/firecracker-containerd/snapshotter/devmapper
sudo cp tools/image-builder/rootfs.img /var/lib/firecracker-containerd/runtime/default-rootfs.img
sudo cp hello-vmlinux.bin /var/lib/firecracker-containerd/runtime/hello-vmlinux.bin
```

#### Step 5: setup snapshotter

Original location: [prepare-and-configure-snapshotter](https://github.com/firecracker-microvm/firecracker-containerd/blob/main/docs/getting-started.md#prepare-and-configure-snapshotter)

```
#!/bin/bash

# Sets up a devicemapper thin pool with loop devices in
# /var/lib/firecracker-containerd/snapshotter/devmapper

set -ex

DIR=/var/lib/firecracker-containerd/snapshotter/devmapper
POOL=fc-dev-thinpool

if [[ ! -f "${DIR}/data" ]]; then
touch "${DIR}/data"
truncate -s 100G "${DIR}/data"
fi

if [[ ! -f "${DIR}/metadata" ]]; then
touch "${DIR}/metadata"
truncate -s 2G "${DIR}/metadata"
fi

DATADEV="$(losetup --output NAME --noheadings --associated ${DIR}/data)"
if [[ -z "${DATADEV}" ]]; then
DATADEV="$(losetup --find --show ${DIR}/data)"
fi

METADEV="$(losetup --output NAME --noheadings --associated ${DIR}/metadata)"
if [[ -z "${METADEV}" ]]; then
METADEV="$(losetup --find --show ${DIR}/metadata)"
fi

SECTORSIZE=512
DATASIZE="$(blockdev --getsize64 -q ${DATADEV})"
LENGTH_SECTORS=$(bc <<< "${DATASIZE}/${SECTORSIZE}")
DATA_BLOCK_SIZE=128 # see https://www.kernel.org/doc/Documentation/device-mapper/thin-provisioning.txt
LOW_WATER_MARK=32768 # picked arbitrarily
THINP_TABLE="0 ${LENGTH_SECTORS} thin-pool ${METADEV} ${DATADEV} ${DATA_BLOCK_SIZE} ${LOW_WATER_MARK} 1 skip_block_zeroing"
echo "${THINP_TABLE}"

if ! $(dmsetup reload "${POOL}" --table "${THINP_TABLE}"); then
dmsetup create "${POOL}" --table "${THINP_TABLE}"
fi
```

Save this script and execute it. For example:

`sudo setup-thinpool.sh`

#### Step 6: before starting containerd

Add the following directories to your path:

```
PATH="$PATH:<path to containerd-firecracker>/runtime"
PATH="$PATH:<path to containerd-firecracker>/firecracker-control/cmd/containerd"
PATH="$PATH:<path to containerd-firecracker>/_submodules/firecracker/build/cargo_target/x86_64-unknown-linux-musl/release"
```

Create a `firecracker-runtime.json` configuration file **(note that you need to edit the firecracker-containerd path)**:

```
{
  "firecracker_binary_path": "<path to firecracker-containerd>/_submodules/firecracker/build/cargo_target/x86_64-unknown-linux-musl/release/firecracker",
  "kernel_image_path": "/var/lib/firecracker-containerd/runtime/hello-vmlinux.bin",
  "kernel_args": "console=ttyS0 noapic reboot=k panic=1 pci=off nomodules ro systemd.unified_cgroup_hierarchy=0 systemd.journald.forward_to_console systemd.unit=firecracker.target init=/sbin/overlay-init",
  "root_drive": "/var/lib/firecracker-containerd/runtime/default-rootfs.img",
  "cpu_template": "T2",
  "log_fifo": "fc-logs.fifo",
  "log_levels": ["info"],
  "metrics_fifo": "fc-metrics.fifo",
  "default_network_interfaces": [
    {
      "CNIConfig": {
        "NetworkName": "fcnet",
        "InterfaceName": "veth0"
      }
    }
  ]
}
```

Create a `firecracker-containerd-config.toml` configuration file:

```
version = 2
disabled_plugins = ["io.containerd.grpc.v1.cri"]
root = "/var/lib/firecracker-containerd/containerd"
state = "/run/firecracker-containerd"
[grpc]
  address = "/run/firecracker-containerd/containerd.sock"
[plugins]
  [plugins."io.containerd.snapshotter.v1.devmapper"]
    pool_name = "fc-dev-thinpool"
    base_image_size = "10GB"
    root_path = "/var/lib/firecracker-containerd/snapshotter/devmapper"

[debug]
  level = "info"
```

#### Step 7: start containerd

```
sudo env PATH=$PATH FIRECRACKER_CONTAINERD_RUNTIME_CONFIG_PATH=firecracker-runtime.json firecracker-containerd --config firecracker-containerd-config.toml
```

This command will launch `containerd-firecracker` and you should now be able to launch Firecracker vms using containerd. For example, the following commands should pull a container image and start a new VM from it:

```
sudo env "PATH=$PATH" firecracker-ctr --address /run/firecracker-containerd/containerd.sock images pull docker.io/library/busybox:latest
sudo env "PATH=$PATH" firecracker-ctr --address /run/firecracker-containerd/containerd.sock run --snapshotter devmapper --runtime aws.firecracker --rm --tty --net-host docker.io/library/busybox:latest

```


#### Step 8: troubleshooting

- you might need to add `--network host` to `docker build` and `docker run` commands in `Makefile`, `tools/image-builder/Makefile`, and `_submodules/firecracker/tools/devtool`;
- you might need to add a nameserver and make sure the private net does not overlap with your current net in `/etc/cni/conf.d/fcnet.conflist`. Here is one working example:

```
{
  "cniVersion": "1.0.0",
  "name": "fcnet",
  "plugins": [
    {
      "type": "bridge",
      "bridge": "fc-br0",
      "isDefaultGateway": true,
      "forceAddress": false,
      "ipMasq": true,
      "hairpinMode": true,
      "mtu": 1500,
      "ipam": {
        "type": "host-local",
        "subnet": "192.168.2.0/24",
        "resolvConf": "/etc/resolv.conf"
      },
      "dns": {
         "nameservers": [ "8.8.8.8" ]
      }
    },
    {
      "type": "firewall"
    },
    {
      "type": "tc-redirect-tap"
    },
    {
      "type": "loopback"
    }
  ]
}
```
