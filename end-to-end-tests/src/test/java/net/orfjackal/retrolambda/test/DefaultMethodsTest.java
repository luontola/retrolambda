// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DefaultMethodsTest {

    /**
     * JDK 8 adds a bridge method to an interface when it overrides a method
     * from the parent interface and refines its return type. This uses Java 8's
     * default methods feature, which won't work on Java 7 and below, so we have
     * to remove it for it - this makes the bytecode same as what JDK 7 produces.
     */
    @Test
    public void will_remove_bridge_methods_from_interfaces() {
        class Foo implements Child {
            @Override
            public String foo() {
                return "foo";
            }
        }
        assertThat("direct call", new Foo().foo(), is("foo"));
        assertThat("bridged call", ((Parent) new Foo()).foo(), is((Object) "foo"));
    }

    public interface Parent {
        Object foo();
    }

    public interface Child extends Parent {
        String foo(); // refined return type
    }
}
