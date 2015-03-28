#!/bin/bash
set -eux

# Test the forking mechanism

mvn clean verify \
    --errors \
    -P fork
mvn clean verify \
    --errors \
    -P fork,noDefaultMethods

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
    -P java6,noDefaultMethods

mvn clean verify \
    --errors \
    -P java5
mvn clean verify \
    --errors \
    -P java5,noDefaultMethods

mvn clean deploy \
    --errors
mvn clean deploy \
    --errors \
    -P noDefaultMethods
