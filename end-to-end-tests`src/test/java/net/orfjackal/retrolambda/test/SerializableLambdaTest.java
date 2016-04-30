// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SerializableLambdaTest {

    @Test
    public void serializable_interface() throws Exception {
        SerializableFoo original = (i) -> i + 10;

        SerializableFoo serialized = roundTripSerialize(original);

        assertThat(serialized.foo(4), is(14));
    }

    @Test
    public void cast_expression_with_serializable_additional_bound() throws Exception {
        Foo original = (Foo & Serializable) (i) -> i + 20;

        Foo serialized = roundTripSerialize(original);

        assertThat(serialized.foo(4), is(24));
    }

    @Test
    public void captured_local_variables() throws Exception {
        int var = 30;
        SerializableFoo original = (i) -> i + var;

        SerializableFoo serialized = roundTripSerialize(original);

        assertThat(serialized.foo(4), is(34));
    }


    // guinea pigs & helpers

    public interface Foo {
        int foo(int i);
    }

    public interface SerializableFoo extends Foo, Serializable {
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTripSerialize(T original) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(original);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
