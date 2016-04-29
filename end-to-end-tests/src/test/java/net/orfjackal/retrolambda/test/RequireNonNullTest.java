// Copyright © 2013-2016 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RequireNonNullTest {

    @Test
    public void requireNonNull__silent_when_non_null() {
        Objects.requireNonNull(new Object());
    }


    @Test(expected = NullPointerException.class)
    public void requireNonNull__throws_NPE_when_null() {
        Objects.requireNonNull(null);
    }

    @Test
    public void requireNonNull__returns_the_argument() {
        Object expected = new Object();

        Object actual = Objects.requireNonNull(expected);

        assertThat(actual, is(sameInstance(expected)));
    }

    @Test
    public void synthetic_null_check__silent_when_non_null() {
        syntheticNullCheck(new MaybeNull());
    }

    @Test(expected = NullPointerException.class)
    public void synthetic_null_check__throws_NPE_when_null() {
        syntheticNullCheck(null);
    }

    @SuppressWarnings("unused")
    private static void syntheticNullCheck(MaybeNull maybeNull) {
        // Javac knows that the `foo` field is constant 0, so it generates a null check and the `iconst_0` instruction.
        // The null check is a `obj.getClass()` call on older JDKs and `Objects.requireNonNull(obj)` on JDK 9 and above.
        int foo = maybeNull.foo;
    }

    private static class MaybeNull {
        final int foo = 0;
    }
}
