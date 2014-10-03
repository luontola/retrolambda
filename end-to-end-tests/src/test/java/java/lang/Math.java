// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package java.lang;

import net.orfjackal.retrolambda.test.ClasspathTest;

/**
 * @see ClasspathTest#ignores_classes_in_explicit_classpath_that_are_under_the_java_package
 */
@SuppressWarnings("UnusedDeclaration")
public class Math {

    public Math() {
        // some lambdas to cause Retrolambda try backporting this class
        Runnable r = () -> {
        };
    }
}
