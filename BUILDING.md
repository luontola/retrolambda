
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


Using the Docker-based Development Environment
----------------------------------------------

To avoid having to install all the Java versions on your computer, there is a
Docker image for building Retrolambda.

The image is already in Docker Hub, but it can also be built locally with the
following command. But first you need to download the JDK 5 and 9 installers
into the `dev/installers` directory and install [Rocker](https://github.com/grammarly/rocker).

    make

To run a shell inside the container:

    make shell
