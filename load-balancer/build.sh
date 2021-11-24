#!/bin/bash

# TODO: try to find latest version.
NGINX_VERSION=1.21.4
OBJ_FILE=ngx_dynamic_upstream_module.so

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
GREEN='\033[0;32m'
NC='\033[0m' # No Color

cd "$DIR" || {
  echo "Redirection fails!"
  exit 1
}

if [ -f configs/${OBJ_FILE} ]; then
  echo -e "${GREEN}${OBJ_FILE} exists! No need to rebuild it!${NC}"
  exit 0
fi

# Pull nginx code.
echo -e "${GREEN}Pulling NGINX code...${NC}"
wget https://nginx.org/download/nginx-${NGINX_VERSION}.tar.gz
echo -e "${GREEN}Pulling NGINX code...done${NC}"

# Extract nginx code.
echo -e "${GREEN}Extracting NGINX code...${NC}"
tar -xzvf nginx-${NGINX_VERSION}.tar.gz
echo -e "${GREEN}Extracting NGINX code...done${NC}"

# Generate .so file.
echo -e "${GREEN}Building shared library...${NC}"
cd nginx-${NGINX_VERSION}/ || {
  echo "Redirection to NGINX fails!"
  exit 1
}
./configure --with-compat --add-dynamic-module=../dynamic_registration
make modules
echo -e "${GREEN}Building shared library...done${NC}"

# Copy it into directory with all configs.
echo -e "${GREEN}Copying libraries into configuration...${NC}"
cp nginx-${NGINX_VERSION}/objs/${OBJ_FILE} cofigs/
echo -e "${GREEN}Copying libraries into configuration...done${NC}"

# Remove .tar.gz and nginx installation.
echo -e "${GREEN}Cleanup...${NC}"
rm nginx-${NGINX_VERSION}.tar.gz
rm -r nginx-${NGINX_VERSION}
echo -e "${GREEN}Cleanup...done${NC}"
