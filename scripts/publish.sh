#!/bin/bash
set -eu
: ${2:? Usage: $0 DESCRIPTION VERSION}
DESCRIPTION="$1"
VERSION="$2"
set -x

mvn nexus-staging:deploy-staged-repository \
    --errors \
    -DrepositoryDirectory=staging \
    -DstagingDescription="$DESCRIPTION"

REPOSITORY_ID=`sed -n -r 's/stagingRepository\.id=(\w+)/\1/p' staging/*.properties`

mvn nexus-staging:release \
    --errors \
    -DaltStagingDirectory=staging \
    -DstagingRepositoryId="$REPOSITORY_ID" \
    -DstagingDescription="$DESCRIPTION"

git push origin HEAD
git push origin --tags

cd ../retrolambda.pages
./update-maven-site.sh "$VERSION"
git push
