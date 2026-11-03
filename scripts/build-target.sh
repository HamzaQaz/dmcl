#!/usr/bin/env bash
# Build one release target and print its jar path as the last line (JAR=<path>).
#   scripts/build-target.sh fabric   <mc>   e.g. 1.21.1, 1.21.11, 26.3
#   scripts/build-target.sh neoforge <mc>
#   scripts/build-target.sh paper    -
# JDKs: set JAVA_HOME_21_X64 / JAVA_HOME_25_X64 (setup-java does this in CI); falls back to JAVA_HOME.
set -euo pipefail
cd "$(dirname "$0")/.."
platform=$1
mc=${2:--}
ver=$(grep '^modVersion' gradle.properties | cut -d= -f2 | tr -d ' \r')

# Minecraft API era -> compat source dir, and the Java the target needs.
compat=1_21_1; java=21
case $mc in
  1.21.[5-9]|1.21.10) compat=1_21_5 ;;
  1.21.11) compat=1_21_11 ;;
  26.3) compat=26; java=25 ;;
  26.*) compat=1_21_11; java=25 ;;
esac
[ "$platform" = paper ] && java=21
if [ "$java" = 25 ]; then export JAVA_HOME=${JAVA_HOME_25_X64:-$JAVA_HOME}; else export JAVA_HOME=${JAVA_HOME_21_X64:-$JAVA_HOME}; fi

latest() { # latest stable <version> in a maven-metadata.xml whose entries match grep pattern $2
  curl -fsS "$1" | grep -oE '<version>[^<]+' | sed 's/<version>//' | grep -E "$2" | grep -viE 'alpha|beta|snapshot' | tail -1
}

case $platform in
  fabric)
    api=$(latest https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml "\\+${mc//./\\.}\$")
    [ -n "$api" ] || { echo "no Fabric API for $mc" >&2; exit 1; }
    extra=()
    [ "$java" = 25 ] && extra=(-Pmc26=1 -PjavaVersion=25 -PloaderVersion=0.20.1)
    ./gradlew build -x installMod -PminecraftVersion="$mc" -Pcompat=$compat -PfabricVersion="$api" "${extra[@]}" --console=plain
    echo "JAR=build/libs/dmcl-fabric-$mc-$ver.jar"
    ;;
  neoforge)
    case $mc in
      26.*.*) prefix="$mc." ;;
      26.*) prefix="$mc.0." ;;
      *) prefix="21.${mc#1.21.}." ;;
    esac
    [ "$mc" = 1.21 ] && prefix="21.0."
    neo=$(latest https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml "^${prefix//./\\.}")
    [ -n "$neo" ] || { echo "no stable NeoForge for $mc" >&2; exit 1; }
    ./gradlew -p neoforge build -PminecraftVersion="$mc" -PneoVersion="$neo" -Pcompat=$compat -PjavaVersion=$java --console=plain
    echo "JAR=neoforge/build/libs/dmcl-neoforge-$mc-$ver.jar"
    ;;
  paper)
    ./gradlew -p paper build --console=plain
    echo "JAR=paper/build/libs/dmcl-paper-$ver.jar"
    ;;
  *) echo "unknown platform $platform" >&2; exit 2 ;;
esac
