#!/usr/bin/env bash

# That script will break after 100 releases (limits in the gh tooling, but until then, it's enough).

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"
cd $DIR/..

FILENAME=CHANGELOG.md
rm CHANGELOG.md

for OUTPUT in $(gh release list | awk -F "\t" '{print $1}'| sort -Vr)
do
  echo -en "# $OUTPUT\n\n" >> $FILENAME
  gh release view $OUTPUT | dos2unix | gsed '1,/^--$/d' | gsed "s/# What's Changed//g" | gsed '1,2{/^$/d}' >> $FILENAME
  echo -en "\n\n\n" >> $FILENAME
done

gsed -i -e :a -e '/^\n*$/{$d;N;ba' -e '}' $FILENAME
