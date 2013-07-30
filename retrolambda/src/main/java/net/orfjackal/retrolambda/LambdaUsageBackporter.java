// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

public class LambdaUsageBackporter {

    private static final int JAVA_8_BYTECODE_VERSION = 52;
    private static final int MAJOR_VERSION_OFFSET = 6;

    private static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";
    private static final Pattern LAMBDA_IMPL_METHOD = Pattern.compile("^lambda\\$\\d+$");

    public static byte[] transform(byte[] bytecode, int targetVersion) {
        asmJava8SupportWorkaround(bytecode);
        resetLambdaClassSequenceNumber();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(bytecode).accept(new MyClassVisitor(cw, targetVersion), 0);
        return cw.toByteArray();
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

    private static void resetLambdaClassSequenceNumber() {
        try {
            Field counterField = Class.forName("java.lang.invoke.InnerClassLambdaMetafactory").getDeclaredField("counter");
            counterField.setAccessible(true);
            AtomicInteger counter = (AtomicInteger) counterField.get(null);
            counter.set(0);
        } catch (Exception e) {
            System.err.println("WARNING: Failed to start class numbering from one. Don't worry, it's cosmetic, " +
                    "but please file a bug report and tell on which JDK version this happened.");
            e.printStackTrace();
        }
    }


    private static class MyClassVisitor extends ClassVisitor {
        private final int targetVersion;
        private String className;

        public MyClassVisitor(ClassWriter cw, int targetVersion) {
            super(ASM4, cw);
            this.targetVersion = targetVersion;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (version > targetVersion) {
                version = targetVersion;
            }
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (LAMBDA_IMPL_METHOD.matcher(name).matches()) {
                access = Flags.makeNonPrivate(access);
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new InvokeDynamicInsnConvertingMethodVisitor(api, mv, className);
        }
    }

    private static class InvokeDynamicInsnConvertingMethodVisitor extends MethodVisitor {
        private final String myClassName;

        public InvokeDynamicInsnConvertingMethodVisitor(int api, MethodVisitor mv, String myClassName) {
            super(api, mv);
            this.myClassName = myClassName;
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (bsm.getOwner().equals(LAMBDA_METAFACTORY)) {
                backportLambda(name, Type.getType(desc), bsm, bsmArgs);
            } else {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            }
        }

        private void backportLambda(String invokedName, Type invokedType, Handle bsm, Object[] bsmArgs) {
            Class<?> invoker = loadClass(myClassName);
            LambdaFactoryMethod factory = LambdaReifier.reifyLambdaClass(invoker, invokedName, invokedType, bsm, bsmArgs);
            super.visitMethodInsn(INVOKESTATIC, factory.getOwner(), factory.getName(), factory.getDesc());
        }

        private static Class<?> loadClass(String className) {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                return cl.loadClass(className.replace('/', '.'));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
