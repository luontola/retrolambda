#!/bin/bash
set -eu
: ${1:? Usage: $0 RELEASE_VERSION}
SCRIPTS=`dirname "$0"`
RELEASE_VERSION="$1"
if [[ ! "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: RELEASE_VERSION must be in X.Y.Z format, but was $RELEASE_VERSION"
    exit 1
fi

function contains-line() {
    grep --line-regexp --quiet --fixed-strings -e "$1"
}

function assert-file-contains() {
    local file="$1"
    local expected="$2"
    cat "$file" | contains-line "$expected" || (echo "Add this line to $file and try again:"; echo "$expected"; exit 1)
}

function bump-version()
{
    [[ $1 =~ ([0-9]+.[0-9]+.)[0-9]+ ]]
    local prefix=${BASH_REMATCH[1]}
    [[ $1 =~ [0-9]+.[0-9]+.([0-9]+) ]]
    local suffix=${BASH_REMATCH[1]}
    ((suffix++))
    echo "$prefix$suffix-SNAPSHOT"
}

APP_NAME="Retrolambda"
NEXT_VERSION=`bump-version $RELEASE_VERSION`

assert-file-contains README.md "### $APP_NAME $RELEASE_VERSION (`date --iso-8601`)"

set -x

mvn versions:set \
    -DgenerateBackupPoms=false \
    -DnewVersion="$RELEASE_VERSION" \
    --file parent/pom.xml
git add -u
git commit -m "Release $RELEASE_VERSION"
git tag -s -m "$APP_NAME $RELEASE_VERSION" "v$RELEASE_VERSION"

$SCRIPTS/stage.sh "$APP_NAME $RELEASE_VERSION"

mvn versions:set \
    -DgenerateBackupPoms=false \
    -DnewVersion="$NEXT_VERSION" \
    --file parent/pom.xml
git add -u
git commit -m "Prepare for next development iteration"

$SCRIPTS/publish.sh "$APP_NAME $RELEASE_VERSION"
