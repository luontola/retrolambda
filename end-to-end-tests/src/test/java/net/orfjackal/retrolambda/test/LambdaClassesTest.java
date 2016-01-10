// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static net.orfjackal.retrolambda.test.TestUtil.assertClassExists;
import static org.junit.Assert.assertTrue;

public class LambdaClassesTest {

    @Test
    public void the_sequence_number_starts_from_1_for_each_enclosing_class() {
        assertClassExists(Dummy1.class.getName() + "$$Lambda$1");
        assertClassExists(Dummy1.class.getName() + "$$Lambda$2");
        assertClassExists(Dummy2.class.getName() + "$$Lambda$1");
        assertClassExists(Dummy2.class.getName() + "$$Lambda$2");
    }

    @Test
    public void capturing_lambda_contain_no_unexpected_methods() throws ClassNotFoundException {
        List<String> expected = new ArrayList<>(Arrays.asList("lambdaFactory$", "run"));
        Class<?> cls = Class.forName(Dummy1.class.getName() + "$$Lambda$1");
        for (Method method : cls.getDeclaredMethods()) {
            assertTrue(method.getName() + " not expected", expected.remove(method.getName()));
        }
        assertTrue("Missing methods: " + expected, expected.isEmpty());
    }

    @Test
    public void non_capturing_lambda_contain_no_unexpected_methods() throws ClassNotFoundException {
        List<String> expected = new ArrayList<>(Arrays.asList("lambdaFactory$", "run"));
        Class<?> cls = Class.forName(Dummy2.class.getName() + "$$Lambda$1");
        for (Method method : cls.getDeclaredMethods()) {
            assertTrue(method.getName() + " not expected", expected.remove(method.getName()));
        }
        assertTrue("Missing methods: " + expected, expected.isEmpty());
    }


    @SuppressWarnings("UnusedDeclaration")
    private class Dummy1 {
        private Dummy1() {
            // Non-capturing lambdas
            Runnable lambda1 = () -> {
            };
            Runnable lambda2 = () -> {
            };
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private class Dummy2 {
        private Dummy2() {
            // Capturing lambdas
            Runnable lambda1 = () -> System.out.println(hashCode());
            Runnable lambda2 = () -> System.out.println(hashCode());
        }
    }
}
