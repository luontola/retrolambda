
Retrolambda: Use Lambdas on Java 7
==================================

Just as there was [Retroweaver](http://retroweaver.sourceforge.net/) et al.
for running Java 5 code with generics on Java 1.4, *Retrolambda* lets you
run Java 8 code with lambda expressions on Java 7. It does this by
transforming your Java 8 compiled bytecode so that it can be run on Java 7
runtime.

This tool backports only lambda expressions - it doesn't let you use the
Java APIs that are new in Java 8.

**WORK IN PROGRESS**
