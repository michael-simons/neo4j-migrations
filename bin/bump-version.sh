#!/usr/bin/env bash

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"

NUM_DOTS=$(echo "${1}" | awk -F. '{ print NF - 1 }')

if [[ "${1}" == *"SNAPSHOT" ]]; then
  if [[ $NUM_DOTS -lt 2 ]]; then
    echo "Version must be either an plain version or a full x.y.z-SNAPSHOT version"
    exit
  fi
  VERSION="${1}"
else
  if [[ $NUM_DOTS -eq 0 ]]; then
    VERSION="${1}.0.0-SNAPSHOT"
  elif [[ $NUM_DOTS -eq 1 ]]; then
    VERSION="${1}.0-SNAPSHOT"
  else
    VERSION="${1}-SNAPSHOT"
  fi
fi

# shellcheck disable=SC2001
MINOR_VERSION=$(echo "${VERSION}" | sed 's/\.[^\.]*$//')
MINOR_VERSION="$MINOR_VERSION\nprerelease: \"true\""

# Make sure there is only pre-release attribute
sed -i .bak '/prerelease: "true"/d' "$DIR"/../docs/antora.yml
rm "$DIR"/../docs/antora.yml.bak

# Replace version
sed -i .bak 's/\(version: &the-version\) \(.*\)/\1 '"${MINOR_VERSION}"'/g' "$DIR"/../docs/antora.yml
rm "$DIR"/../docs/antora.yml.bak
./mvnw -f "$DIR"/../pom.xml versions:set -DgenerateBackupPoms=false -DnewVersion="${VERSION}"
