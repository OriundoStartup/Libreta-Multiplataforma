#!/bin/bash
set -e

echo "Descargando Amazon Corretto 17..."
mkdir -p /tmp/jdk17
curl -sL "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz" \
  | tar -xz -C /tmp/jdk17 --strip-components=1

export JAVA_HOME=/tmp/jdk17
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java version:"
java -version

chmod +x gradlew
./gradlew :composeApp:wasmJsBrowserDistribution \
  --no-daemon \
  --no-configuration-cache \
  --warning-mode none \
  -Dfile.encoding=UTF-8
