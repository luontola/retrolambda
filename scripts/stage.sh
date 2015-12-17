#!/bin/bash
set -eu
: ${1:? Usage: $0 DESCRIPTION}
SCRIPTS=`dirname "$0"`
DESCRIPTION="$1"
set -x

rm -rfv staging

mvn clean deploy \
    --errors \
    -P sonatype-oss-release \
    -DaltDeploymentRepository="staging::default::file:staging"
