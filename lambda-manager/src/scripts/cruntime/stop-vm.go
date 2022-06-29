package main

import (
	"context"
	"flag"
	"github.com/containerd/containerd"
	"github.com/containerd/containerd/namespaces"
	fcclient "github.com/firecracker-microvm/firecracker-containerd/firecracker-control/client"
	"github.com/firecracker-microvm/firecracker-containerd/proto"
	"github.com/pkg/errors"
	"log"
	"syscall"
)

func main() {
	var containerdAddr = flag.String("cnt", "/run/firecracker-containerd/containerd.sock", "containerd address. Example: -cnt /run/containerd/containerd.sock")
	var ttrpcCAddr = *containerdAddr + ".ttrpc"
	var namespace = flag.String("ns", "firecracker-containerd-example", "namespace. Example: -ns firecracker-containerd-example")
	var vmID = flag.String("id", "", "vm ID. Example: -id fc-example")

	log.SetFlags(log.Ldate | log.Ltime | log.Lmicroseconds)
	flag.Parse()

	if *vmID == "" {
		log.Fatal("Incorrect usage. 'id' needs to be specified")
	}

	if err := stop(*containerdAddr, ttrpcCAddr, *namespace, *vmID); err != nil {
		log.Fatal("failed to stop VM, err: %v\n", err)
	}
}

func stop(containerdAddr string, ttrpcCAddr string, namespace string, vmid string) (err error) {
	client, err := containerd.New(containerdAddr)
	if err != nil {
		return errors.Wrapf(err, "creating client")
	}

	ctx := namespaces.WithNamespace(context.Background(), namespace)

	fcClient, err := fcclient.New(ttrpcCAddr)
	if err != nil {
		return err
	}

	container, err := client.LoadContainer(ctx, "demo")
	if err != nil {
		return errors.Wrapf(err, "error loading container")
	}

	task, err := container.Task(ctx, nil)
	if err != nil {
		return errors.Wrapf(err, "error loading task")
	}

	exitStatusC, err := task.Wait(ctx)
	if err != nil {
		return errors.Wrapf(err, "waiting for task")
	}

	if err := task.Kill(ctx, syscall.SIGTERM, containerd.WithKillAll); err != nil {
		return errors.Wrapf(err, "killing task")
	}

	status := <-exitStatusC
	code, _, err := status.Result()
	if err != nil {
		return errors.Wrapf(err, "getting task's exit code")
	}
	log.Printf("task exited with status: %d\n", code)

	task.Delete(ctx)

	container.Delete(ctx, containerd.WithSnapshotCleanup)

	_, err = fcClient.StopVM(ctx, &proto.StopVMRequest{VMID: vmid})
	if err != nil {
		log.Printf("failed to stop VM, err: %v\n", err)
	}

	fcClient.Close()
	client.Close()
	return nil
}
