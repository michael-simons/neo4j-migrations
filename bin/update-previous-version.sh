#!/usr/bin/env bash

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"

NEW_OLD_VERSION=$(sed -n 's/project\.rel\.eu\.michael-simons\.neo4j\\:neo4j-migrations-parent=\(.*\)/\1/p' $DIR/../release.properties)
$DIR/../mvnw versions:set-property -DgenerateBackupPoms=false -Dproperty=neo4j-migrations.previous.version -DnewVersion=$NEW_OLD_VERSION -pl :neo4j-migrations-parent
