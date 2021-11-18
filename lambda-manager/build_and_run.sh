#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
GREEN='\033[0;32m'
NC='\033[0m' # No Color

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

echo -e "${GREEN}Building lambda manager...${NC}"
./gradlew clean assemble
echo -e "${GREEN}Building lambda manager...done${NC}"

echo -e "${GREEN}Running lambda manager...${NC}"
sudo java -jar build/libs/lambda-manager-1.0-all.jar
echo -e "${GREEN}Running lambda manager...done${NC}"
