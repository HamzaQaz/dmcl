#!/usr/bin/env bash
# Build a NeoForge target: ./scripts-matrix-neoforge.sh <mc>   (JAVA_HOME must be 21, or 25 for 26.x)
set -e
mc=$1
case $mc in
  1.21.1|1.21.2|1.21.3|1.21.4) compat=1_21_1; java=21 ;;
  1.21.5|1.21.6|1.21.7|1.21.8|1.21.9|1.21.10) compat=1_21_5; java=21 ;;
  1.21.11) compat=1_21_11; java=21 ;;
  26.3) compat=26; java=25 ;;
  26.*) compat=1_21_11; java=25 ;;
esac
case $mc in
  1.21) prefix="21.0." ;;
  1.21.*) prefix="21.${mc#1.21.}." ;;
  26.*) prefix="$mc." ; [ "$(echo $mc | tr -cd . | wc -c)" = 1 ] && prefix="$mc.0." ;;
esac
neo=$(curl -s https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml | grep -oE "<version>[^<]+" | sed 's/<version>//' | grep -E "^${prefix//./\.}" | grep -vE "beta|alpha" | tail -1)
echo "== $mc neo=$neo compat=$compat java=$java"
[ -z "$neo" ] && { echo "no stable NeoForge for $mc"; exit 0; }
./gradlew -p neoforge build -PminecraftVersion=$mc -PneoVersion=$neo -Pcompat=$compat -PjavaVersion=$java --console=plain 2>&1 | grep -E "error:|BUILD|What went wrong" -A2 | head -14
