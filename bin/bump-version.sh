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

# Replace version
# ...in Antora playbook
sed -i .bak \
  -e 's/^\(version:\) \(.*\)/\1 "'"${MINOR_VERSION}"'"/g' \
  -e 's/\(prerelease:\) .*/\1 "SNAPSHOT"/g' \
  "$DIR"/../docs/antora.yml
rm "$DIR"/../docs/antora.yml.bak
# ...in Maven build descriptor
./mvnw -f "$DIR"/../pom.xml versions:set -DgenerateBackupPoms=false -DnewVersion="${VERSION}"
