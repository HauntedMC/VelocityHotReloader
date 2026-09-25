#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
version="$(sed -n 's/.*<revision>\([0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*\)<\/revision>.*/\1/p' pom.xml | head -n 1)"
[[ -n "$version" ]] || { echo 'Missing semantic Maven revision' >&2; exit 1; }
artifact="target/VelocityHotReloader-$version.jar"
[[ -f "$artifact" ]] || { echo "Missing distributable $artifact" >&2; exit 1; }
entries="$(jar tf "$artifact")"
descriptor="$(unzip -p "$artifact" velocity-plugin.json)"
grep -Fq "\"version\":\"$version\"" <<<"$descriptor"
grep -Fq 'nl/hauntedmc/velocityhotreloader/velocity/dependencies/gson/Gson.class' <<<"$entries"
grep -Fq 'nl/hauntedmc/velocityhotreloader/velocity/dependencies/adventure/text/minimessage/MiniMessage.class' <<<"$entries"
if grep -Eq '^com/google/gson/|^net/kyori/adventure/text/minimessage/|^com/velocitypowered/|^com/mojang/brigadier/|^com/electronwill/nightconfig/' <<<"$entries"; then
  echo 'Plugin jar contains unrelocated or provided classes' >&2
  exit 1
fi
echo "Artifact audit passed: $artifact"
