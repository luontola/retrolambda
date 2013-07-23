#!/bin/bash
set -eu
: ${1:? Usage: $0 RELEASE_VERSION}
SCRIPTS=`dirname "$0"`

RELEASE_VERSION="$1"
if [[ ! "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: RELEASE_VERSION must be in X.Y.Z format, but was $RELEASE_VERSION"
    exit 1
fi

function bump_version()
{
    local prefix=`echo $1 | sed -n -r 's/([0-9]+\.[0-9]+\.)[0-9]+/\1/p'`
    local suffix=`echo $1 | sed -n -r 's/[0-9]+\.[0-9]+\.([0-9]+)/\1/p'`
    ((suffix++))
    echo "$prefix$suffix-SNAPSHOT"
}
NEXT_VERSION=`bump_version $RELEASE_VERSION`
set -x

mvn versions:set \
    -DgenerateBackupPoms=false \
    -DnewVersion="$RELEASE_VERSION" \
    --file parent/pom.xml
git add -u
git commit -m "Release $RELEASE_VERSION"
git tag -s -m "Retrolambda $RELEASE_VERSION" "v$RELEASE_VERSION"

$SCRIPTS/stage.sh "Retrolambda $RELEASE_VERSION"

mvn versions:set \
    -DgenerateBackupPoms=false \
    -DnewVersion="$NEXT_VERSION" \
    --file parent/pom.xml
git add -u
git commit -m "Prepare for next development iteration"

$SCRIPTS/publish.sh "Retrolambda $RELEASE_VERSION"
