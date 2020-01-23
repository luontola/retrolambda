#!/bin/bash
set -eu

# If fork fails because the Java agent cannot be installed,
# it will log an error and use the fallback technique.
# We need to check the logs for an error to detect the failure.
check_build_log () {
    grep ERROR build.log && exit 1
    rm build.log
}

set -x

# Run end-to-end tests against all supported Java versions

## Java 7

mvn clean verify \
    --errors \
    -P noDefaultMethods \
    | tee build.log && check_build_log

mvn clean verify \
    --errors \
    | tee build.log && check_build_log

## Java 6

mvn clean verify \
    --errors \
    -P java6,noDefaultMethods \
    | tee build.log && check_build_log

mvn clean verify \
    --errors \
    -P java6 \
    | tee build.log && check_build_log

## Java 5

mvn clean verify \
    --errors \
    -P java5,noDefaultMethods \
    | tee build.log && check_build_log

mvn clean verify \
    --errors \
    -P java5 \
    | tee build.log && check_build_log

# Test the forking mechanism

mvn clean verify \
    --errors \
    -P fork,noDefaultMethods \
    | tee build.log && check_build_log

mvn clean verify \
    --errors \
    -P fork \
    | tee build.log && check_build_log

# The Maven plugin's minimum requirement is Java 6,
# but then the plugin must force forking the process

JAVA_HOME="$JAVA6_HOME" mvn clean verify \
    --errors \
    -P java6 \
    | tee build.log && check_build_log

# Java 9 has stricter bytecode validation than Java 8,
# so make sure that Retrolambda can run under new Java versions (without forking)

JAVA_HOME="$JAVA9_HOME" mvn clean verify \
    --errors \
    | tee build.log && check_build_log

JAVA_HOME="$JAVA10_HOME" mvn clean verify \
    --errors \
    | tee build.log && check_build_log

JAVA_HOME="$JAVA11_HOME" mvn clean verify \
    --errors \
    | tee build.log && check_build_log

# (Java 12+ fails without forking because java.lang.invoke.InnerClassLambdaMetafactory#dumper cannot be made non-final)

# Make sure that the Java agent works on all new Java versions

JAVA_HOME="$JAVA9_HOME" mvn clean verify \
    --errors \
    -P fork,noToolchain \
    | tee build.log && check_build_log

JAVA_HOME="$JAVA10_HOME" mvn clean verify \
    --errors \
    -P fork,noToolchain \
    | tee build.log && check_build_log

JAVA_HOME="$JAVA11_HOME" mvn clean verify \
    --errors \
    -P fork,noToolchain \
    | tee build.log && check_build_log

JAVA_HOME="$JAVA12_HOME" mvn clean verify \
    --errors \
    -P fork,noToolchain \
    | tee build.log && check_build_log

JAVA_HOME="$JAVA13_HOME" mvn clean verify \
    --errors \
    -P fork,noToolchain \
    | tee build.log && check_build_log
