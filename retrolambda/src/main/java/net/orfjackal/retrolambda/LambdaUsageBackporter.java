// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;

public class LambdaUsageBackporter {

    private static final int JAVA_8_BYTECODE_VERSION = 52;
    private static final int MAJOR_VERSION_OFFSET = 6;

    private static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";
    private static final Pattern LAMBDA_IMPL_METHOD = Pattern.compile("^lambda\\$\\d+$");

    public static byte[] transform(byte[] bytecode) {
        asmJava8SupportWorkaround(bytecode);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
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
        private final List<LambdaFactoryMethod> lambdaFactoryMethods = new ArrayList<>();

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
            if (LAMBDA_IMPL_METHOD.matcher(name).matches()) {
                access = Flags.makeNonPrivate(access);
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new InvokeDynamicInsnConvertingMethodVisitor(api, mv, myClassName, lambdaFactoryMethods);
        }

        @Override
        public void visitEnd() {
            for (LambdaFactoryMethod factoryMethod : lambdaFactoryMethods) {
                factoryMethod.generateMethod(cv);
            }
            super.visitEnd();
        }
    }

    private static class InvokeDynamicInsnConvertingMethodVisitor extends MethodVisitor {
        private final String myClassName;
        private final List<LambdaFactoryMethod> lambdaFactoryMethods;

        public InvokeDynamicInsnConvertingMethodVisitor(int api, MethodVisitor mv, String myClassName, List<LambdaFactoryMethod> lambdaFactoryMethods) {
            super(api, mv);
            this.myClassName = myClassName;
            this.lambdaFactoryMethods = lambdaFactoryMethods;
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (bsm.getOwner().equals(LAMBDA_METAFACTORY)) {
                try {
                    backportLambda(name, Type.getType(desc), bsm, bsmArgs);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            } else {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            }
        }

        private void backportLambda(String invokedName, Type invokedType, Handle bsm, Object[] bsmArgs) throws Throwable {
            Class<?> invoker = Class.forName(myClassName.replace('/', '.'));
            callBootstrapMethod(invoker, invokedName, invokedType, bsm, bsmArgs);
            String lambdaClass = LambdaSavingClassFileTransformer.getLastFoundLambdaClass();

            LambdaFactoryMethod factoryMethod = new LambdaFactoryMethod(invokedType, lambdaClass);
            lambdaFactoryMethods.add(factoryMethod);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, myClassName, factoryMethod.getName(), invokedType.getDescriptor());
        }

        private static CallSite callBootstrapMethod(Class<?> invoker, String invokedName, Type invokedType, Handle bsm, Object[] bsmArgs) throws Throwable {
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

        private static MethodHandles.Lookup getLookup(Class<?> targetClass) throws Exception {
            Constructor<MethodHandles.Lookup> ctor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            ctor.setAccessible(true);
            return ctor.newInstance(targetClass);
        }

        private static Object asmToInvokerType(Object arg, ClassLoader classLoader, MethodHandles.Lookup caller) throws Exception {
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

        private static MethodHandle toMethodHandle(Handle handle, ClassLoader classLoader, MethodHandles.Lookup lookup) throws Exception {
            MethodType type = MethodType.fromMethodDescriptorString(handle.getDesc(), classLoader);
            Class<?> owner = classLoader.loadClass(handle.getOwner().replace('/', '.'));

            if (handle.getTag() == Opcodes.H_INVOKESTATIC) {
                return lookup.findStatic(owner, handle.getName(), type);

            } else if (handle.getTag() == Opcodes.H_INVOKESPECIAL) {
                return lookup.findSpecial(owner, handle.getName(), type, owner);

            } else if (handle.getTag() == Opcodes.H_INVOKEVIRTUAL
                    || handle.getTag() == Opcodes.H_INVOKEINTERFACE) {
                return lookup.findVirtual(owner, handle.getName(), type);

            } else {
                throw new AssertionError("unexpected tag: " + handle.getTag());
            }
        }
    }

    private static class LambdaFactoryMethod {
        private final Type invokedType;
        private final String lambdaClass;

        public LambdaFactoryMethod(Type invokedType, String lambdaClass) {
            this.invokedType = invokedType;
            this.lambdaClass = lambdaClass;
        }

        public void generateMethod(ClassVisitor cv) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    getName(), invokedType.getDescriptor(), null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, lambdaClass);
            mv.visitInsn(Opcodes.DUP);
            int varIndex = 0;
            for (Type type : invokedType.getArgumentTypes()) {
                mv.visitVarInsn(getLoadInsn(type), varIndex);
                varIndex += type.getSize();
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, lambdaClass, "<init>", withVoidReturnType(invokedType));
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0); // rely on COMPUTE_MAXS
            mv.visitEnd();
        }

        public String getName() {
            return "lambdaFactory$" + lambdaClass.replaceFirst(".+\\$\\$Lambda\\$", "");
        }

        private static int getLoadInsn(Type type) {
            switch (type.getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    return Opcodes.ILOAD;
                case Type.LONG:
                    return Opcodes.LLOAD;
                case Type.FLOAT:
                    return Opcodes.FLOAD;
                case Type.DOUBLE:
                    return Opcodes.DLOAD;
                default:
                    return Opcodes.ALOAD;
            }
        }

        private static String withVoidReturnType(Type type) {
            return Type.getMethodType(Type.VOID_TYPE, type.getArgumentTypes()).getDescriptor();
        }
    }
}
