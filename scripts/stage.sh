#!/bin/bash
set -eu
: ${1:? Usage: $0 DESCRIPTION}
DESCRIPTION="$1"
set -x

rm -rfv staging

mvn clean verify \
    --errors \
    -P fork

mvn clean verify \
    --errors \
    -P java6

mvn clean verify \
    --errors \
    -P java5

mvn clean deploy \
    --errors \
    -P sonatype-oss-release \
    -DaltDeploymentRepository="staging::default::file:staging"

mvn nexus-staging:deploy-staged-repository \
    --errors \
    -DrepositoryDirectory=staging \
    -DstagingDescription="$DESCRIPTION"
