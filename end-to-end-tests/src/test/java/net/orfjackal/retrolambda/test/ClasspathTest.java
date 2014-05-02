// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.hamcrest.*;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ClasspathTest {

    @Test
    public void maven_plugin_sets_classpath_for_main_dependencies() {
        assertThat(InMainSources.useLambdaOfImportedType(Arrays.asList("a", "b")), is(Arrays.asList("A", "B")));
    }

    @Test
    public void maven_plugin_sets_classpath_for_test_dependencies() {
        SelfDescribing lambda = (desc) -> desc.appendText("foo");

        StringDescription result = new StringDescription();
        lambda.describeTo(result);
        assertThat(result.toString(), is("foo"));
    }
}
