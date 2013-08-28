
Building Retrolambda
====================

Set the environment variables `JAVA8_HOME` and `JAVA7_HOME` (optionally also
`JAVA6_HOME` and `JAVA5_HOME`) point to the installation directories of those
particular JDK versions.

Compile the project with Maven using the command:

    mvn clean verify

To run the tests using Java 6 and Java 5, use the commands:

    mvn clean verify -P java6
    mvn clean verify -P java5

Finally copy the executable JAR from the `retrolambda/target/` directory.
