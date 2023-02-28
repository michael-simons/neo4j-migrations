#!/usr/bin/env bash

# Call this with ./generate-site.sh targetFolder branchName

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"
TARGET_FOLDER=$1
SOURCE_BRANCH=$2

cd "$DIR"/..

# Generate the documentation
./mvnw --no-transfer-progress asciidoctor:process-asciidoc@generate-docs -pl :neo4j-migrations-parent -Dproject.build.docs="$TARGET_FOLDER" -Dproject.build.docs.branch="$SOURCE_BRANCH"
# Generate the site
# The asciidoc extensions fails for some weird Ruby / JRuby errors on GitHub; test results modified as they depend on that module as well.
sed -i.bak -e '/<module>extensions\/neo4j-migrations-formats-adoc<\/module>/d' -e '/<module>neo4j-migrations-test-results<\/module>/d' pom.xml
./mvnw --no-transfer-progress -q -Dfast -Dmaven.javadoc.skip=false package site:site site:stage
mv pom.xml.bak pom.xml

# Copy the site
rm -rf "$TARGET_FOLDER"/site &&
mv target/staging "$TARGET_FOLDER"/site
