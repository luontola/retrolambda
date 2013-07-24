// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class OptimizationsTest {

    @Test
    public void lambdas_which_capture_variables_get_a_new_instance_every_time() {
        Callable<Integer> lambda1 = createStatefulLambda();
        Callable<Integer> lambda2 = createStatefulLambda();

        assertThat(lambda1, is(not(sameInstance(lambda2))));
    }

    private static Callable<Integer> createStatefulLambda() {
        int i = 42;
        return () -> i;
    }

    @Test
    public void lambdas_which_do_not_capture_variables_have_only_one_singleton_instance() {
        Callable<Integer> lambda1 = createStatelessLambda();
        Callable<Integer> lambda2 = createStatelessLambda();

        assertThat(lambda1, is(sameInstance(lambda2)));
    }

    private static Callable<Integer> createStatelessLambda() {
        return () -> 42;
    }
}
