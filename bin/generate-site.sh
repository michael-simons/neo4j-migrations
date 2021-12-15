#!/usr/bin/env bash

# Call this with ./generate-site.sh targetFolder branchName

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"
TARGET_FOLDER=$1
SOURCE_BRANCH=$2

cd $DIR/..

# Generate the documentation
./mvnw --no-transfer-progress asciidoctor:process-asciidoc@generate-docs -pl :neo4j-migrations-parent -Dproject.build.docs=$TARGET_FOLDER -Dproject.build.docs.branch=$SOURCE_BRANCH

# For whatever reasons  maven-project-info-reports-plugin:3.1.2:dependencies goes nuts on that one
# it's a pom project, there will be no classes, but apparently the plugin thinks different
mkdir -p neo4j-migrations-spring-boot-starter-parent/neo4j-migrations-spring-boot-starter/target/classes
./mvnw --no-transfer-progress -q compile site:site site:stage

# Copy the site
rm -rf $TARGET_FOLDER/site &&
mv target/staging $TARGET_FOLDER/site
