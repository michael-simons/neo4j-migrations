#!/usr/bin/env bash

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"

cd $DIR/..
# For whatever reasons  maven-project-info-reports-plugin:3.1.2:dependencies goes nuts on that one
# it's a pom project, there will be no classes, but apparently the plugin thinks different
mkdir -p neo4j-migrations-spring-boot-starter-parent/neo4j-migrations-spring-boot-starter/target/classes
./mvnw -B -q compile site:site site:stage
