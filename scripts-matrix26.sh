#!/usr/bin/env bash
# Build Fabric 26.x target: ./scripts-matrix26.sh <mc>   (needs JDK 25 as JAVA_HOME)
set -e
mc=$1; compat=1_21_11; [ "$mc" = "26.3" ] && compat=26
api=$(curl -s https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml | grep -oE "<version>[^<]+\+$mc</version>" | tail -1 | sed 's/<[^>]*>//g')
echo "== $mc api=$api"
./gradlew build -x installMod -Pmc26=1 -PminecraftVersion=$mc -Pcompat=$compat -PjavaVersion=25 -PloaderVersion=0.20.1 -PfabricVersion=$api --console=plain 2>&1 | grep -E "error:|BUILD" -A2 | head -12
