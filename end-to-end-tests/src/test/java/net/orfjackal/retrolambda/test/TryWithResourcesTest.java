// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.io.Closeable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TryWithResourcesTest {

    @Test
    public void suppressed_exceptions() {
        try {
            try (ThrowSecondaryExceptionOnClose c = new ThrowSecondaryExceptionOnClose()) {
                throw new PrimaryException();
            }

        } catch (Exception e) {
            assertThat("thrown", e, is(instanceOf(PrimaryException.class)));
            assertThat("cause", e.getCause(), is(nullValue()));

            // On Java 6 and lower we will swallow the suppressed exception, because the API does not exist,
            // but on Java 7 we want to keep the original behavior.
            if (SystemUtils.isJavaVersionAtLeast(1.7f)) {
                assertThat("suppressed", e.getSuppressed(), arrayContaining(instanceOf(SecondaryException.class)));
            }
        }
    }


    private static class PrimaryException extends RuntimeException {
    }

    private static class SecondaryException extends RuntimeException {
    }

    private static class ThrowSecondaryExceptionOnClose implements Closeable {
        @Override
        public void close() {
            throw new SecondaryException();
        }
    }
}
