#!/bin/zsh
set -e

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <release-version> <next-development-version>"
    exit 1
fi

mvn versions:set -DnewVersion="$1"
mvn versions:set-property -Dproperty=prefab.version -DnewVersion="$1"
mvn clean package
git add .
git commit -m "Release version $1"
git tag "$1"
git push origin main --tags
mvn deploy -DskipTests
mvn versions:set -DnewVersion="$2-SNAPSHOT"
mvn versions:set-property -Dproperty=prefab.version -DnewVersion="$2-SNAPSHOT"
git add .
git commit -m "Start next development iteration"
git push origin main