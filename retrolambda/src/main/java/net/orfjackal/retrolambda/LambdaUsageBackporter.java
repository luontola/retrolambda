// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.*;

public class LambdaUsageBackporter {

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
        private String myClassName;

        public MyClassVisitor(ClassWriter cw) {
            super(Opcodes.ASM4, cw);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.myClassName = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new InvokeDynamicInsnConvertingMethodVisitor(api, mv, myClassName);
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
            // TODO: remove debug code
            System.out.println("visitInvokeDynamicInsn\n" +
                    "\t" + name + "\n" +
                    "\t" + desc + "\n" +
                    "\t" + bsm + "\n" +
                    "\t" + Arrays.toString(bsmArgs));
            System.out.println("bsm.getDesc() = " + bsm.getDesc());
            System.out.println("bsm.getName() = " + bsm.getName());
            System.out.println("bsm.getOwner() = " + bsm.getOwner());
            System.out.println("bsm.getTag() = " + bsm.getTag());

            if (bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                backportLambda(name, Type.getType(desc), bsm, bsmArgs);
            } else {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            }
        }

        private void backportLambda(String invokedName, Type invokedType, Handle bsm, Object[] bsmArgs) {
            try {
                Class<?> invoker = Class.forName(myClassName.replace('/', '.'));
                callBootstrapMethod(invoker, invokedName, invokedType, bsm, bsmArgs);
                String lambdaClass = LambdaSavingClassFileTransformer.getLastFoundLambdaClass();

                // TODO: constructor parameters for lambda
                super.visitTypeInsn(Opcodes.NEW, lambdaClass);
                super.visitInsn(Opcodes.DUP);
                super.visitMethodInsn(Opcodes.INVOKESPECIAL, lambdaClass, "<init>", "()V");

            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        private CallSite callBootstrapMethod(Class<?> invoker, String invokedName, Type invokedType, Handle bsm, Object[] bsmArgs) throws Throwable {
            ClassLoader cl = invoker.getClassLoader();
            MethodHandles.Lookup caller = getLookup(invoker);

            List<Object> args = new ArrayList<>();
            args.add(caller);
            args.add(invokedName);
            args.add(toMethodType(invokedType, cl));
            for (Object arg : bsmArgs) {
                args.add(asmToInvokerType(arg, cl, caller));
            }

            MethodHandle bootstrapMethod = toMethodHandle(bsm, cl, caller);
            return (CallSite) bootstrapMethod.invokeWithArguments(args);
        }
    }

    private static MethodHandles.Lookup getLookup(Class<?> targetClass) {
        try {
            Constructor<MethodHandles.Lookup> ctor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            ctor.setAccessible(true);
            return ctor.newInstance(targetClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object asmToInvokerType(Object arg, ClassLoader classLoader, MethodHandles.Lookup caller) {
        if (arg instanceof Type) {
            return toMethodType((Type) arg, classLoader);
        } else if (arg instanceof Handle) {
            return toMethodHandle((Handle) arg, classLoader, caller);
        } else {
            return arg;
        }
    }

    private static MethodType toMethodType(Type type, ClassLoader classLoader) {
        return MethodType.fromMethodDescriptorString(type.getInternalName(), classLoader);
    }

    private static MethodHandle toMethodHandle(Handle handle, ClassLoader classLoader, MethodHandles.Lookup lookup) {
        try {
            MethodType type = MethodType.fromMethodDescriptorString(handle.getDesc(), classLoader);
            Class<?> owner = classLoader.loadClass(handle.getOwner().replace('/', '.'));
            if (handle.getTag() == Opcodes.H_INVOKESTATIC) {
                return lookup.findStatic(owner, handle.getName(), type);
            } else {
                throw new AssertionError("unexpected tag: " + handle.getTag());
            }

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
