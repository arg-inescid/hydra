#!/bin/bash

# TODO: try to find latest version.
NGINX_VERSION=1.21.4
OBJ_FILE=ngx_dynamic_upstream_module.so

if [ -f configs/${OBJ_FILE} ]; then
    echo "${OBJ_FILE} exists! No need to rebuild it!"
    exit 0
fi

# Pull nginx code.
wget https://nginx.org/download/nginx-${NGINX_VERSION}.tar.gz

# Extract nginx code.
tar -xzvf nginx-${NGINX_VERSION}.tar.gz

# Generate .so file.
cd nginx-${NGINX_VERSION}/
./configure --with-compat --add-dynamic-module=../dynamic_registration
make modules

# Copy it into directory with all configs.
cp nginx-${NGINX_VERSION}/objs/${OBJ_FILE} cofigs/

# Remove .tar.gz and nginx installation.
rm nginx-${NGINX_VERSION}.tar.gz
rm -r nginx-${NGINX_VERSION}
