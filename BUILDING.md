
Building Retrolambda
====================

Set the following environment variables to point to the installation
directories of those particular JDK versions:
`JAVA8_HOME`, `JAVA7_HOME`, `JAVA6_HOME`, `JAVA5_HOME`

Create ~/.m2/toolchains.xml and list all the JDKs there as shown in
http://maven.apache.org/plugins/maven-toolchains-plugin/toolchains/jdk.html

Compile the project with Maven using the command:

    mvn clean verify

To run the tests using Java 6 and Java 5, use the commands:

    mvn clean verify -P java6
    mvn clean verify -P java5

To run all combinations of test configurations, use the script:

    ./scripts/build.sh

Finally copy the executable JAR from the `retrolambda/target/` directory.
