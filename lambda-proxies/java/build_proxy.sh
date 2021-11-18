#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
GREEN='\033[0;32m'
NC='\033[0m' # No Color

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

echo -e "${GREEN}Building proxy...${NC}"
mvn package
echo -e "${GREEN}Building proxy...done${NC}"