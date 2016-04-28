
Jake's Retrolambda
==================

This fork of [Retrolambda](https://github.com/orfjackal/retrolambda) integrates a few PRs I have
made upstream which are not yet merged and/or released:

 *  [**Detect and omit the JVM's lambda factory method.**](https://github.com/orfjackal/retrolambda/pull/82)

    Retrolambda uses Java 8's LambdaMetaFactory to materialize lambdas into classes at compile-time
    (which is what the JVM otherwise does at runtime). There are two extra methods on these classes
    which are not really needed: a serialization hook for replacing the body of the serialized form
    and a factory method for creating instances of the class. Retrolambda always removed the former
    method. This change removes the latter, since Retrolambda generates its own factory method on
    the materialized class.

 *  [**Alter when accessor methods are generated.**](https://github.com/orfjackal/retrolambda/pull/84)

    By default, Retrolambda generates a package-private accessor method in the host class when
    materializing method references into classes. This ensures the generated class (which is also in
    the same package) can invoke the accessor method. When the method reference is not a private
    method, though, this accessor does not need to be generated.

    Additionally, when a protected method is referenced from a base class, an additional accessor
    method needs to be generated. This allows the generated class to invoke the method through the
    accessor which is otherwise only visible to the subclass.

 *  [**Remove NEW, DUP instructions when changing NEWINVOKESPECIAL to INVOKESTATIC.**](https://github.com/orfjackal/retrolambda/pull/85)

    When a lambda calls a private method from the host class (or its enclosed or enclosing types),
    the `invokespecial` bytecode is used. When Retrolambda materializes the lambda to a class it
    generates an accessor method that calls through to the private method and uses that instead.
    When the private method is a constructor there are two additional bytecodes, `new` and `dup`,
    which preceded `invokespecial`. This changes the behavior of Retrolambda from appending `pop`
    bytecodes to discard the instance created by these `new` and `dup` bytecodes to actually
    just removing them completely.

 *  [**Skip access method when lambda body method can be promoted.**](https://github.com/orfjackal/retrolambda/pull/86)

    Lambda bodies are moved to private static methods when materialized. In order to invoke them
    from them class that Retrolambda generates an additional package-private static accessor method
    had to be generated in the class. Instead, the private method is promoted to package-private
    visibility so that it can be invoked directly from the new class.

These changes have saved approximately 1500-2000 methods from being generated for our app.

This fork also integrates PRs from other contributors made upstream which are not yet merged:

 *  [**Rewrite calls to Objects.requireNonNull**](https://github.com/orfjackal/retrolambda/pull/93)

    When compiling with JDK 9, `javac` uses the Java 7-only `Objects.requireNonNull` method for
    nullability checks. In order to be compatible with pre-Java 7 runtimes, this is rewritten
    to call `getClass()` on the instance which is in line with pre-JDK 9 behavior.

To use this version of Retrolambda in your build (with Evan T's plugin) add the following:
```groovy
dependencies {
  retrolambdaConfig 'com.jakewharton.retrolambda:retrolambda:2.1.0-jake2'
}
```


Version History
---------------

### Retrolambda 2.1.0-jake2 *(2016-04-27)*

 * Fix: Do not generate accessor methods for protected method references in base classes when
   the base class is in the same package.


### Retrolambda 2.1.0-jake1 *(2016-04-27)*

Initial release.
