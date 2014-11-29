// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class TryWithResourcesTest {

    @Test
    public void calls_close() throws IOException {
        Closeable closeable = mock(Closeable.class);

        try (Closeable c = closeable) {
        }

        verify(closeable).close();
    }

    @Test
    public void suppressed_exceptions() {
        Exception thrown;
        try {
            try (ThrowSecondaryExceptionOnClose c = new ThrowSecondaryExceptionOnClose()) {
                throw new PrimaryException();
            }
        } catch (Exception e) {
            thrown = e;
        }

        assertThat("thrown", thrown, is(instanceOf(PrimaryException.class)));
        assertThat("cause", thrown.getCause(), is(nullValue()));

        // On Java 6 and lower we will swallow the suppressed exception, because the API does not exist,
        // but on Java 7 we want to keep the original behavior.
        if (SystemUtils.isJavaVersionAtLeast(1.7f)) {
            assertThat("suppressed", thrown.getSuppressed(), arrayContaining(instanceOf(SecondaryException.class)));
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
