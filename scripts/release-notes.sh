#!/usr/bin/env bash
# Per-target Modrinth release notes: target details + that version's CHANGELOG.md section.
#   scripts/release-notes.sh <platform> <mc> <version>
set -euo pipefail
cd "$(dirname "$0")/.."
platform=$1; mc=$2; ver=$3
case $platform in
  fabric)   name="Fabric";   mcline="Minecraft $mc"; java=21; [[ $mc == 26.* ]] && java=25
            req="Requires [Fabric API](https://modrinth.com/mod/fabric-api) and Fabric Loader 0.16 or newer." ;;
  neoforge) name="NeoForge"; mcline="Minecraft $mc"; java=21; [[ $mc == 26.* ]] && java=25
            req="Requires NeoForge for Minecraft $mc." ;;
  paper)    name="Paper";    mcline="Paper 1.21 and later"; java=21
            req="Works on Paper (and forks that keep the Paper API). Drop it in \`plugins/\`." ;;
esac
echo "**DMCL $ver for $name ($mcline)**"
echo
echo "- Java $java or newer"
echo "- $req"
echo "- Server-side only: players do not need to install anything."
echo "- Setup guide: https://github.com/HamzaQaz/dmcl#install"
echo
awk -v v="$ver" '$0=="## "v {f=1; next} /^## /{f=0} f' CHANGELOG.md
