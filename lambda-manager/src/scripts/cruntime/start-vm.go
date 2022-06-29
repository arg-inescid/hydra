package main

import (
	"context"
	"flag"
	"github.com/containerd/containerd"
	"github.com/containerd/containerd/cio"
	"github.com/containerd/containerd/namespaces"
	"github.com/containerd/containerd/oci"
	fcclient "github.com/firecracker-microvm/firecracker-containerd/firecracker-control/client"
	"github.com/firecracker-microvm/firecracker-containerd/proto"
	"github.com/firecracker-microvm/firecracker-containerd/runtime/firecrackeroci"
	"github.com/pkg/errors"
	"log"
)

func main() {
	var containerCIDR = flag.String("ip", "", "ip address and subnet assigned to the container in CIDR notation. Example: -ip 172.16.0.2/24")
	var gatewayIP = flag.String("gw", "", "gateway ip address. Example: -gw 172.16.0.1")
	var containerdAddr = flag.String("cnt", "/run/firecracker-containerd/containerd.sock", "containerd address. Example: -cnt /run/containerd/containerd.sock")
	var namespace = flag.String("ns", "firecracker-containerd-example", "namespace. Example: -ns firecracker-containerd-example")
	var vmID = flag.String("id", "fc-example", "vm ID. Example: -id fc-example")
	var tapName = flag.String("tap", "", "tap. Example: -tap tap001")
	var macAddr = flag.String("mac", "AA:FC:00:00:00:01", "tap. Example: -mac AA:FC:00:00:00:01")
	var ttrpcCAddr = *containerdAddr + ".ttrpc"
	var ctnImage = flag.String("img", "docker.io/library/nginx:1.17-alpine", "img. Example: -img docker.io/library/nginx:1.17-alpine")

	log.SetFlags(log.Ldate | log.Ltime | log.Lmicroseconds)
	flag.Parse()

	if *containerCIDR == "" || *gatewayIP == "" || *vmID == "" || *tapName == "" {
		log.Fatal("Incorrect usage. You need to specify the ip, gateway, id, and tap.")
	}

	if err := start(*containerdAddr, ttrpcCAddr, *namespace, *vmID, *containerCIDR, *gatewayIP, "devmapper", *tapName, *macAddr, *ctnImage); err != nil {
		log.Fatal(err)
	}
}

func start(containerdAddr string, ttrpcCAddr string, namespace string, vmID string, containerCIDR string, gateway string, snapshotter string, tapName string, macAddr string, ctnImage string) (err error) {
	log.Println("Creating containerd client")
	client, err := containerd.New(containerdAddr)
	if err != nil {
		return errors.Wrapf(err, "creating client")
	}

	log.Println("Created containerd client")

	ctx := namespaces.WithNamespace(context.Background(), namespace)

	// In alternative to pull: client.GetImage(ctx, ctnImage)
	image, err := client.Pull(ctx, ctnImage, containerd.WithPullUnpack, containerd.WithPullSnapshotter(snapshotter))
	if err != nil {
		return errors.Wrapf(err, "preparing image")
	}

	fcClient, err := fcclient.New(ttrpcCAddr)
	if err != nil {
		return err
	}

	createVMRequest := &proto.CreateVMRequest{
		VMID: vmID,
		// Enabling Go Race Detector makes in-microVM binaries heavy in terms of CPU and memory.
		MachineCfg: &proto.FirecrackerMachineConfiguration{
			VcpuCount:  2,
			MemSizeMib: 2048,
		},
	}

	createVMRequest.NetworkInterfaces = []*proto.FirecrackerNetworkInterface{{
		StaticConfig: &proto.StaticNetworkConfiguration{
			MacAddress:  macAddr,
			HostDevName: tapName,
			IPConfig: &proto.IPConfiguration{
				PrimaryAddr: containerCIDR,
				GatewayAddr: gateway,
			},
		},
	}}

	_, err = fcClient.CreateVM(ctx, createVMRequest)
	if err != nil {
		return errors.Wrap(err, "failed to create VM")
	}

	log.Printf("Successfully pulled %s image with %s\n", image.Name(), snapshotter)
	container, err := client.NewContainer(
		ctx,
		"demo",
		containerd.WithSnapshotter(snapshotter),
		containerd.WithNewSnapshot("demo-snapshot", image),
		containerd.WithNewSpec(
			oci.WithImageConfig(image),
			firecrackeroci.WithVMID(vmID),
			firecrackeroci.WithVMNetwork,
		),
		containerd.WithRuntime("aws.firecracker", nil),
	)
	if err != nil {
		return err
	}

	task, err := container.NewTask(ctx, cio.NewCreator(cio.WithStdio))
	if err != nil {
		return errors.Wrapf(err, "creating task")

	}

	log.Printf("Successfully created task: %s for the container\n", task.ID())

	if err := task.Start(ctx); err != nil {
		return errors.Wrapf(err, "starting task")

	}

	log.Println("Successfully started the container task")
	fcClient.Close()
	client.Close()
	return err
}
