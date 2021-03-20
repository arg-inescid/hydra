#!/bin/bash -e

url_decode() {
  : "${*//+/ }"
  echo -e "${_//%/\\x}"
}

# Caching credentials.
sudo ls > /dev/null

LATEST_GRAAL_URL=$(curl -s "https://api.github.com/repos/graalvm/graalvm-ce-dev-builds/releases" |
  grep -Po "['\"]browser_download_url['\"]\s*:\s*['\"]\K(.*)(?=['\"])" |
  grep -m1 "graalvm-ce-java11-linux-amd64-.*-dev.tar.gz")

LATEST_GRAAL_FILENAME=$(url_decode "$(basename "$LATEST_GRAAL_URL")")

if [ ! -f "$LATEST_GRAAL_FILENAME" ]; then
  echo "Downloading GraalVM..."
  wget --continue --quiet "$LATEST_GRAAL_URL"
  echo "Downloading GraalVM...done"
fi

LATEST_GRAAL_DIR=$(tar -tzf "$LATEST_GRAAL_FILENAME" | head -1 | cut -f1 -d"/")

if [ ! -d "$LATEST_GRAAL_DIR" ]; then
  tar -xzf "$LATEST_GRAAL_FILENAME" -C "."
fi

mv "$LATEST_GRAAL_DIR" graalvm

export GRAALVM_HOME="$PWD/graalvm"
export JAVA_HOME="$GRAALVM_HOME"
export PATH="$GRAALVM_HOME/bin:$PATH"

# Installing docker (https://docs.docker.com/engine/install/ubuntu/)
echo "Installing docker..."
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates curl wget gnupg-agent software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo apt-key fingerprint 0EBFCD88
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io
echo "Installing docker...done"

# Installing the world
echo "Installing the world..."
sudo aptitude install gcc libz-dev libguestfs-tools gnuplot maven qemu qemu-kvm apache2-utils
echo "Installing the world...done"

# Installing native-image
echo "Installing native-image..."
gu install native-image
echo "Installing native-image...done"

echo "JAVA_HOME=$JAVA_HOME"
