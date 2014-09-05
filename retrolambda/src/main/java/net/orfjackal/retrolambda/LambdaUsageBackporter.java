// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.defaultmethods.*;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class LambdaUsageBackporter {

    public static byte[] transform(byte[] bytecode, int targetVersion) {
        resetLambdaClassSequenceNumber();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        if (FeatureToggles.DEFAULT_METHODS == 1) {
            ClassModifier stage3 = new ClassModifier(targetVersion, writer);
            InterfaceModifier stage2 = new InterfaceModifier(stage3, targetVersion);
            InvokeDynamicInsnConverter stage1 = new InvokeDynamicInsnConverter(stage2, targetVersion);
            new ClassReader(bytecode).accept(stage1, 0);
        } else {
            InvokeDynamicInsnConverter stage1 = new InvokeDynamicInsnConverter(writer, targetVersion);
            new ClassReader(bytecode).accept(stage1, 0);
        }
        return writer.toByteArray();
    }

    private static void resetLambdaClassSequenceNumber() {
        try {
            Field counterField = Class.forName("java.lang.invoke.InnerClassLambdaMetafactory").getDeclaredField("counter");
            counterField.setAccessible(true);
            AtomicInteger counter = (AtomicInteger) counterField.get(null);
            counter.set(0);
        } catch (Throwable t) {
            // print to stdout to keep in sync with other log output
            System.out.println("WARNING: Failed to start class numbering from one. Don't worry, it's cosmetic, " +
                    "but please file a bug report and tell on which JDK version this happened.");
            t.printStackTrace(System.out);
        }
    }


    private static class InvokeDynamicInsnConverter extends ClassVisitor {
        private final int targetVersion;
        private int classAccess;
        String className;
        private final Map<Handle, Handle> lambdaBridgesToImplMethods = new LinkedHashMap<>();

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
            if (FeatureToggles.DEFAULT_METHODS == 0
                    && isNonAbstractMethodOnInterface(access)
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
            return new InvokeDynamicInsnConvertingMethodVisitor(mv, this);
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

        Handle getLambdaBridgeMethod(Handle implMethod) {
            if (!implMethod.getOwner().equals(className)) {
                return implMethod;
            }
            // TODO: do not generate a bridge method if the impl method is not private (probably not implementable with a single pass)
            String name = "access$lambda$" + lambdaBridgesToImplMethods.size();
            String desc = implMethod.getTag() == H_INVOKESTATIC
                    ? implMethod.getDesc()
                    : Types.prependArgumentType(Type.getType("L" + className + ";"), implMethod.getDesc());
            Handle bridgeMethod = new Handle(H_INVOKESTATIC, className, name, desc);
            lambdaBridgesToImplMethods.put(bridgeMethod, implMethod);
            return bridgeMethod;
        }

        @Override
        public void visitEnd() {
            for (Map.Entry<Handle, Handle> entry : lambdaBridgesToImplMethods.entrySet()) {
                Handle bridgeMethod = entry.getKey();
                Handle implMethod = entry.getValue();
                generateLambdaBridgeMethod(bridgeMethod, implMethod);
            }
            super.visitEnd();
        }

        private void generateLambdaBridgeMethod(Handle bridge, Handle impl) {
            MethodVisitor mv = super.visitMethod(ACC_STATIC | ACC_SYNTHETIC | ACC_BRIDGE,
                    bridge.getName(), bridge.getDesc(), null, null);
            mv.visitCode();
            int varIndex = 0;
            for (Type type : Type.getArgumentTypes(bridge.getDesc())) {
                mv.visitVarInsn(type.getOpcode(ILOAD), varIndex);
                varIndex += type.getSize();
            }
            mv.visitMethodInsn(Handles.getOpcode(impl), impl.getOwner(), impl.getName(), impl.getDesc(), impl.getTag() == H_INVOKEINTERFACE);
            mv.visitInsn(Type.getReturnType(bridge.getDesc()).getOpcode(IRETURN));
            mv.visitMaxs(-1, -1); // rely on ClassWriter.COMPUTE_MAXS
            mv.visitEnd();
        }
    }

    private static class InvokeDynamicInsnConvertingMethodVisitor extends MethodVisitor {
        private final InvokeDynamicInsnConverter context;

        public InvokeDynamicInsnConvertingMethodVisitor(MethodVisitor mv, InvokeDynamicInsnConverter context) {
            super(ASM5, mv);
            this.context = context;
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
            Class<?> invoker = loadClass(context.className);
            Handle implMethod = (Handle) bsmArgs[1];
            Handle bridgeMethod = context.getLambdaBridgeMethod(implMethod);

            LambdaFactoryMethod factory = LambdaReifier.reifyLambdaClass(implMethod, bridgeMethod,
                    invoker, invokedName, invokedType, bsm, bsmArgs);
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
}
