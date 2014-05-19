// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class LambdaUsageBackporter {

    public static byte[] transform(byte[] bytecode, int targetVersion) {
        resetLambdaClassSequenceNumber();

        MethodVisibilityAdjuster stage2 = new MethodVisibilityAdjuster();
        InvokeDynamicInsnConverter stage1 = new InvokeDynamicInsnConverter(stage2, targetVersion);
        new ClassReader(bytecode).accept(stage1, 0);
        stage2.makePackagePrivate(stage1.lambdaImplMethods);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        stage2.accept(cw);
        return cw.toByteArray();
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


    private static class InvokeDynamicInsnConverter extends ClassVisitor {
        private final int targetVersion;
        private int classAccess;
        private String className;
        public final List<Handle> lambdaImplMethods = new ArrayList<>();

        public InvokeDynamicInsnConverter(ClassVisitor next, int targetVersion) {
            super(ASM5, next);
            this.targetVersion = targetVersion;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (version > targetVersion) {
                version = targetVersion;
            }
            super.visit(version, access, name, signature, superName, interfaces);
            this.classAccess = access;
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (isBridgeMethodOnInterface(access)) {
                return null; // remove the bridge method; Java 7 didn't use them
            }
            if (isNonAbstractMethodOnInterface(access)
                    && !isClassInitializerMethod(name, desc, access)) {
                // In case we have missed a case of Java 8 producing non-abstract methods
                // on interfaces, we have this warning here to get a bug report sooner.
                // Not allowed by Java 7:
                // - default methods
                // - static methods
                // - bridge methods
                // Allowed by Java 7:
                // - class initializer methods (for initializing constants)
                System.out.println("WARNING: Method '" + name + "' of interface '" + className + "' is non-abstract! " +
                        "This will probably fail to run on Java 7 and below. " +
                        "If you get this warning _without_ using Java 8's default methods, " +
                        "please report a bug at https://github.com/orfjackal/retrolambda/issues " +
                        "together with an SSCCE (http://www.sscce.org/)");
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new InvokeDynamicInsnConvertingMethodVisitor(mv, className, lambdaImplMethods);
        }

        private boolean isBridgeMethodOnInterface(int methodAccess) {
            return Flags.hasFlag(classAccess, ACC_INTERFACE) &&
                    Flags.hasFlag(methodAccess, ACC_BRIDGE);
        }

        private boolean isNonAbstractMethodOnInterface(int methodAccess) {
            return Flags.hasFlag(classAccess, ACC_INTERFACE) &&
                    !Flags.hasFlag(methodAccess, ACC_ABSTRACT);
        }

        private static boolean isClassInitializerMethod(String name, String desc, int methodAccess) {
            return name.equals("<clinit>") &&
                    desc.equals("()V") &&
                    Flags.hasFlag(methodAccess, ACC_STATIC);
        }
    }

    private static class InvokeDynamicInsnConvertingMethodVisitor extends MethodVisitor {
        private final String myClassName;
        private final List<Handle> lambdaImplMethods;

        public InvokeDynamicInsnConvertingMethodVisitor(MethodVisitor mv, String myClassName, List<Handle> lambdaImplMethods) {
            super(ASM5, mv);
            this.myClassName = myClassName;
            this.lambdaImplMethods = lambdaImplMethods;
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (bsm.getOwner().equals(LambdaNaming.LAMBDA_METAFACTORY)) {
                backportLambda(name, Type.getType(desc), bsm, bsmArgs);
            } else {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            }
        }

        private void backportLambda(String invokedName, Type invokedType, Handle bsm, Object[] bsmArgs) {
            Class<?> invoker = loadClass(myClassName);
            Handle lambdaImplMethod = (Handle) bsmArgs[1];
            lambdaImplMethods.add(lambdaImplMethod);
            LambdaFactoryMethod factory = LambdaReifier.reifyLambdaClass(lambdaImplMethod, invoker, invokedName, invokedType, bsm, bsmArgs);
            super.visitMethodInsn(INVOKESTATIC, factory.getOwner(), factory.getName(), factory.getDesc(), false);
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

    private static class MethodVisibilityAdjuster extends ClassNode {

        public MethodVisibilityAdjuster() {
            super(ASM5);
        }

        public void makePackagePrivate(List<Handle> targetMethods) {
            for (MethodNode method : this.methods) {
                if (contains(method, targetMethods)) {
                    method.access = Flags.makeNonPrivate(method.access);
                }
            }
        }

        private boolean contains(MethodNode needle, List<Handle> haystack) {
            for (Handle handle : haystack) {
                if (handle.getOwner().equals(this.name) &&
                        handle.getName().equals(needle.name) &&
                        handle.getDesc().equals(needle.desc)) {
                    return true;
                }
            }
            return false;
        }
    }
}
