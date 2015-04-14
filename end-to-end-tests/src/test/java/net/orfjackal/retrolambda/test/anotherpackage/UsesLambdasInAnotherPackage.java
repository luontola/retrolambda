// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test.anotherpackage;

import java.util.concurrent.Callable;

public interface UsesLambdasInAnotherPackage {

    default Callable<String> stateless() {
        return () -> "foo";
    }

    default Callable<String> captureThis() {
        return () -> stateless().call();
    }
}
