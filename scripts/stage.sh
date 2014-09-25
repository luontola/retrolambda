#!/bin/bash
set -eu
: ${1:? Usage: $0 DESCRIPTION}
DESCRIPTION="$1"
set -x

rm -rfv staging

# Test that also the forking mechanism works

mvn clean verify \
    --errors \
    -P fork

# The Maven plugin's minimum requirement is Java 6,
# but then the plugin must force forking the process

JAVA_HOME="$JAVA6_HOME" mvn clean verify \
    --errors \
    -P java6

# Run end-to-end tests against all supported Java versions

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
