#!/usr/bin/env bash
# Compile-check Fabric targets: ./scripts-matrix.sh <mc> <compat>
set -e
mc=$1; compat=$2
api=$(curl -s https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml | grep -oE "<version>[^<]+\+$mc</version>" | tail -1 | sed 's/<[^>]*>//g')
echo "== $mc compat=$compat api=$api"
./gradlew build -x installMod -PminecraftVersion=$mc -Pcompat=$compat -PfabricVersion=$api --console=plain 2>&1 | grep -E "error:|BUILD" -A2 | head -12
