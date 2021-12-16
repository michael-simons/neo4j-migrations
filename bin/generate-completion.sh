#!/usr/bin/env bash
#
# Generates shell completion scripts. Required parameter is the classpath as assembled by the Maven build.
# Two scripts will be generated: The standard PicoCLI one that can be used as standard bash completion script
# or source'd both in bash and zsh as well as an autoload function to be used with zsh alone (via homebrew).
# The alias defined initially is ignored respectively gets lost

set -euo pipefail

java $1 $2 $3 $4 > $5/neo4j-migrations_completion
cp $5/neo4j-migrations_completion $5/_neo4j-migrations

ZSH_SHIM="
compopt() {
  complete \$@
}

_complete_neo4j-migrations
"

echo "$ZSH_SHIM" >> $5/_neo4j-migrations
