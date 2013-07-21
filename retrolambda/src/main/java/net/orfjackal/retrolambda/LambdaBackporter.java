// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.nio.ByteBuffer;

public class LambdaBackporter {

    private static final int JAVA_8_BYTECODE_VERSION = 52;
    private static final int MAJOR_VERSION_OFFSET = 6;

    public static byte[] transform(byte[] bytecode) {
        asmJava8SupportWorkaround(bytecode);
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytecode).accept(new MyClassVisitor(writer), 0);
        return writer.toByteArray();
    }

    private static void asmJava8SupportWorkaround(byte[] bytecode) {
        ByteBuffer buffer = ByteBuffer.wrap(bytecode);
        short majorVersion = buffer.getShort(MAJOR_VERSION_OFFSET);

        if (majorVersion == JAVA_8_BYTECODE_VERSION) {
            // XXX: ASM doesn't yet support Java 8, so we must fake the data to be from Java 7
            buffer.putShort(MAJOR_VERSION_OFFSET, (short) (majorVersion - 1));
            // TODO: once we can remove this workaround, make our ClassVisitor responsible for setting the bytecode version

        } else if (majorVersion > JAVA_8_BYTECODE_VERSION) {
            throw new IllegalArgumentException("Only Java 8 and lower is supported, but bytecode version was " + majorVersion);
        }
    }

    private static class MyClassVisitor extends ClassVisitor {

        public MyClassVisitor(ClassWriter cw) {
            super(Opcodes.ASM4, cw);
        }
    }
}
