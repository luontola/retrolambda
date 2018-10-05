// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.nio.charset.*;
import java.util.*;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class ApiMapperTest {

    @Test
    public void toUnsignedLong() {

        assertThat(Integer.toUnsignedLong(0xffffffff), equalTo(4294967295L));
    }

    @Test
    public void predicates() {
        assertThat(filter(Arrays.asList("Hello World", "Goodbye World"), x -> x.startsWith("Hello")), hasSize(1));
    }

    private List<String> filter(List<String> strings, Predicate<String> predicate) {
        List<String> result = new ArrayList<>();
        for (String string : strings) {
            if(predicate.test(string)) {
                result.add(string);
            }
        }
        return result;
    }
}
