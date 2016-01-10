// Copyright © 2013-2016 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static net.orfjackal.retrolambda.test.TestUtil.assertClassExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LambdaClassesTest {

    @Test
    public void the_sequence_number_starts_from_1_for_each_enclosing_class() {
        assertClassExists(Dummy1.class.getName() + "$$Lambda$1");
        assertClassExists(Dummy1.class.getName() + "$$Lambda$2");
        assertClassExists(Dummy2.class.getName() + "$$Lambda$1");
        assertClassExists(Dummy2.class.getName() + "$$Lambda$2");
    }

    @SuppressWarnings("UnusedDeclaration")
    private class Dummy1 {
        private Dummy1() {
            Runnable lambda1 = () -> {
            };
            Runnable lambda2 = () -> {
            };
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private class Dummy2 {
        private Dummy2() {
            Runnable lambda1 = () -> {
            };
            Runnable lambda2 = () -> {
            };
        }
    }


    @Test
    public void capturing_lambda_classes_contain_no_unnecessary_methods() throws ClassNotFoundException {
        Set<String> expected = new HashSet<>(Arrays.asList("lambdaFactory$", "run"));

        Class<?> lambdaClass = Class.forName(Capturing.class.getName() + "$$Lambda$1");

        Set<String> actual = new HashSet<>();
        for (Method method : lambdaClass.getDeclaredMethods()) {
            actual.add(method.getName());
        }
        assertThat(actual, is(expected));
    }

    @SuppressWarnings("UnusedDeclaration")
    private class Capturing {
        private Capturing() {
            Runnable lambda = () -> System.out.println(hashCode());
        }
    }


    @Test
    public void non_capturing_lambda_classes_contain_no_unnecessary_methods() throws ClassNotFoundException {
        Set<String> expected = new HashSet<>(Arrays.asList("lambdaFactory$", "run"));

        Class<?> lambdaClass = Class.forName(NonCapturing.class.getName() + "$$Lambda$1");

        Set<String> actual = new HashSet<>();
        for (Method method : lambdaClass.getDeclaredMethods()) {
            actual.add(method.getName());
        }
        assertThat(actual, is(expected));
    }

    @SuppressWarnings("UnusedDeclaration")
    private class NonCapturing {
        private NonCapturing() {
            Runnable lambda = () -> {
            };
        }
    }
}
