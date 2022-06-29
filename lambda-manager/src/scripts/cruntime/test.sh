#!/bin/bash

go build start-vm.go
go build stop-vm.go

sudo ./start-vm -ip 192.168.1.99/24 -gw 192.168.1.83 -tap tap001 -id fc-example2 -img docker.io/library/nginx:1.17-alpine

sleep 1

curl 192.168.1.99

sudo ./stop-vm -id fc-example2
