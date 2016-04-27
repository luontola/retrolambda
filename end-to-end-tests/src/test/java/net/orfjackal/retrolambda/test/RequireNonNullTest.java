// Copyright 2016 The Retrolambda Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.orfjackal.retrolambda.test;

import com.google.common.base.*;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import java.io.*;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class RequireNonNullTest {

    static class Foo {
        Object test(Object x) {
            return Objects.requireNonNull(x);
        }
    }

    @Test
    public void throwsNPE() throws Exception {
        byte[] bytes;
        String path = Foo.class.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            bytes = ByteStreams.toByteArray(is);
        }
        String actual = dumpMethod(bytes, "test");
        if (SystemUtils.isJavaVersionAtLeast(1.7f)) {
            assertEquals(
                    "// access flags 0x0\n"
                    + "test(Ljava/lang/Object;)Ljava/lang/Object;\n"
                    + "ALOAD 1\n"
                    + "INVOKESTATIC java/util/Objects.requireNonNull (Ljava/lang/Object;)Ljava/lang/Object;\n"
                    + "ARETURN\n"
                    + "MAXSTACK = 1\n"
                    + "MAXLOCALS = 2",
                    actual);
        } else {
             assertEquals(
                    "// access flags 0x0\n"
                    + "test(Ljava/lang/Object;)Ljava/lang/Object;\n"
                    + "ALOAD 1\n"
                    + "DUP\n"
                    + "INVOKEVIRTUAL java/lang/Object.getClass ()Ljava/lang/Class;\n"
                    + "POP\n"
                    + "ARETURN\n"
                    + "MAXSTACK = 2\n"
                    + "MAXLOCALS = 2",
                    actual);
        }
    }

    static String dumpMethod(byte[] bytes, final String methodName) throws Exception {
        Textifier textifier = new Textifier();
        StringWriter sw = new StringWriter();
        final ClassVisitor tcv = new TraceClassVisitor(null, textifier, new PrintWriter(sw, true));
        ClassVisitor cv =
                new ClassVisitor(Opcodes.ASM5) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String desc,
                            String signature,
                            String[] exceptions) {
                        if (!name.equals(methodName)) {
                            return super.visitMethod(access, name, desc, signature, exceptions);
                        }
                        return tcv.visitMethod(access, name, desc, signature, exceptions);
                    }
                };
        ClassReader cr = new ClassReader(bytes);
        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        textifier.print(new PrintWriter(sw, true));
        return Joiner.on('\n')
                .join(Splitter.on('\n').omitEmptyStrings().trimResults().split(sw.toString()));
    }
}
