#!/bin/bash
set -eu
: ${1:? Usage: $0 VERSION}
VERSION="$1"
set -x

git clone ../retrolambda/.git/ tmp
(cd tmp && git checkout "v$VERSION" && mvn site)
rm -rf retrolambda-maven-plugin
cp -rv tmp/retrolambda-maven-plugin/target/site retrolambda-maven-plugin
rm -rf tmp

git reset
git add -A retrolambda-maven-plugin
git commit -m "Maven plugin docs for version $VERSION"
