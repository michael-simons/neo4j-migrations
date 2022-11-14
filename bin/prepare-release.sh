#!/usr/bin/env bash

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"

sed -i .bak 's/\(:version:\) \(.*\)/\1 '"${1}"'/g' "$DIR"/../README.adoc
rm "$DIR"/../README.adoc.bak

# shellcheck disable=SC2001
MINOR_VERSION=$(echo "${1}" | sed 's/\.[^\.]*$//')
sed -i .bak \
  -e 's/\(version: &the-version\) \(.*\)/\1 "'"${MINOR_VERSION}"'"/g' \
  -e 's/\(prerelease:\) .*/\1 "false"/g' \
  "$DIR"/../docs/antora.yml
rm "$DIR"/../docs/antora.yml.bak

if test -n "${2-}"; then
  DRYRUN=$2
else
  DRYRUN='false'
fi

if [ "$DRYRUN" != "true" ]; then
  git add "$DIR"/../README.adoc
  git commit -m "[maven-release-plugin] update README.adoc"
fi
