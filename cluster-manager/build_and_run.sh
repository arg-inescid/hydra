#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
GREEN='\033[0;32m'
NC='\033[0m' # No Color

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

echo -e "${GREEN}Building cluster manager...${NC}"
./gradlew clean assemble
echo -e "${GREEN}Building cluster manager...done${NC}"

echo -e "${GREEN}Running cluster manager...${NC}"
java -jar build/libs/cluster-manager-1.0-all.jar
echo -e "${GREEN}Running cluster manager...done${NC}"
