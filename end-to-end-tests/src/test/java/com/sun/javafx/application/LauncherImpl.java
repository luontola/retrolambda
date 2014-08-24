// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package com.sun.javafx.application;

import net.orfjackal.retrolambda.test.ClasspathTest;

/**
 * @see ClasspathTest#prefers_classes_in_explicit_classpath_over_classes_in_the_JRE
 */
@SuppressWarnings("UnusedDeclaration")
public class LauncherImpl {

    public Runnable foo() {
        return () -> {
        };
    }
}
