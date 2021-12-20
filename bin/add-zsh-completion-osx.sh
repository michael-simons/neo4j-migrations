#!/usr/bin/env bash

set -euo pipefail

cp $1/$2 $1/_neo4j-migrations

ZSH_SHIM="
compopt() {
  complete \$@
}

_complete_neo4j-migrations
"

echo "$ZSH_SHIM" >> $1/_neo4j-migrations
