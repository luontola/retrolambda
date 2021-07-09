// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Function;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class FunctionTest {
    @Test
    public void convertFunction() throws Throwable {
        InputStream is = Function.class.getResourceAsStream("Function.class");
        assertNotNull("Bytecode of Function found", is);

        byte[] oldB = readFully(is);

        ClassAnalyzer analyzer = new ClassAnalyzer();
        Transformers transformers = new Transformers(Opcodes.V1_7, true, analyzer);
        try {
            byte[] newB = transformers.backportClass(new ClassReader(oldB));
            assertNotNull("No exception", newB);
            Assert.assertNotEquals("Different", oldB, newB);
        } catch (RuntimeException ex) {
            Throwable t = ex;
            while (t != null) {
                if (t instanceof IllegalArgumentException) {
                    throw t;
                }
                t = t.getCause();
            }
        }
    }

    static byte[] readFully(InputStream is) throws IOException {
        if (is == null) {
            throw new IOException();
        }
        byte[] arr = new byte[4096 * 4096];
        int off = 0;
        for (;;) {
            int len = is.read(arr, off, arr.length - off);
            if (len == -1) {
                break;
            }
            off += len;
        }
        return Arrays.copyOf(arr, off);
    }
}

