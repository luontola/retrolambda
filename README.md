
Retrolambda: Use Lambdas on Java 7
==================================

Just as there was [Retroweaver](http://retroweaver.sourceforge.net/) et al.
for running Java 5 code with generics on Java 1.4, **Retrolambda** lets you
run Java 8 code with lambda expressions on Java 7 or lower. It does this by
transforming your Java 8 compiled bytecode so that it can run on a Java 7
runtime.

Adventurous developers may use Retrolambda to backport lambda expressions
even to Java 6 or Java 5. And if you reach Java 5, there are [other
backporting tools](http://en.wikipedia.org/wiki/Java_backporting_tools)
that may let you go down to Java 1.4.


User Guide
----------

[Download](https://oss.sonatype.org/content/repositories/releases/net/orfjackal/retrolambda/retrolambda/)
the latest retrolambda.jar from Maven Central.

Use JDK 8 to compile your source code.

Run Retrolambda, using Java 8, on the class files produced by JDK 8. Run
`java -jar retrolambda.jar` without any additional options to see the
instructions. For an example of how to do this with Maven, see how
maven-dependency-plugin and maven-antrun-plugin are used in
[end-to-end-tests/pom.xml](https://github.com/orfjackal/retrolambda/blob/master/end-to-end-tests/pom.xml)

Your class files should now run on Java 7. Be sure to run comprehensive tests on
Java 7, in case the code accidentally uses Java 8 APIs or language features that
Retrolambda doesn't backport.


Compiling
---------

Set the environment variables `JAVA8_HOME` and `JAVA7_HOME` (optionally also
`JAVA6_HOME` and `JAVA5_HOME`) point to the installation directories of those
particular JDK versions.

Compile the project with Maven using the command:

    mvn clean verify

To run the tests using Java 6 and Java 5, use the commands:

    mvn clean verify -P java6
    mvn clean verify -P java5

Finally copy the executable JAR from the `retrolambda/target/` directory.


Known Limitations
-----------------

Does not backport the use of Java 8 APIs.

Does not backport Java 8 language features other than lambda expressions.

Does not support serializable lambda expressions. Implementing support for
them would technically be possible, but it would require projects to have a
runtime dependency on a library which would contain a backported copy of
the `java.lang.invoke.SerializedLambda` class. If you really need it, make
a feature request. ;-)

May break if a future JDK 8 build stops generating a new class for each
`invokedynamic` call. Retrolambda works so that it captures the bytecode
that `java.lang.invoke.LambdaMetafactory` generates dynamically, so
optimizations to that mechanism may break Retrolambda.


Version History
---------------

**Retrolambda 1.0.0 (2013-07-23)**

- Backports lambda expressions and method handles to Java 7 and older
- Tested to work with JDK 8 Early Access Build b99 (2013-07-19)
