// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import java.util.concurrent.Callable;

public class InMainSources {

    public static int callLambda() throws Exception {
        Callable<Integer> lambda = () -> 42;
        return lambda.call();
    }
}
