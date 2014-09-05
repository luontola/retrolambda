// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("Convert2MethodRef")
public class InterfaceStaticMethodsTest {

    @Test
    public void static_methods_on_interfaces() {
        assertThat(Interface.staticMethod(), is(42));
    }

    @Test
    public void static_methods_on_interfaces_taking_arguments() {
        assertThat(Interface.staticMethodWithArgs("a", 1, 2L), is("a12"));
    }

    @Test
    public void calling_static_methods_on_interfaces_from_default_methods() {
        Interface obj = new Interface() {
        };
        assertThat(obj.callStaticMethod(), is(42));
    }

    @Test
    public void calling_static_methods_on_interfaces_from_lambdas() throws Exception {
        Callable<Integer> c = () -> Interface.staticMethod();
        assertThat(c.call(), is(42));
    }

    @Test
    public void calling_static_methods_on_interfaces_from_method_references() throws Exception {
        Callable<Integer> c = Interface::staticMethod;
        assertThat(c.call(), is(42));
    }

    private interface Interface {
        default int callStaticMethod() {
            return staticMethod();
        }

        static int staticMethod() {
            return 42;
        }

        // arguments of just a couple of different types because we're lazy
        static String staticMethodWithArgs(String s, int a, long b) {
            return s + a + b;
        }
    }
}
