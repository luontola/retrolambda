// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class BackportLambdaInvocations extends ClassVisitor {

    private int classAccess;
    private String className;
    private final Map<Handle, Handle> lambdaBridgesToImplMethods = new LinkedHashMap<>();

    public BackportLambdaInvocations(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        resetLambdaClassSequenceNumber();
        this.classAccess = access;
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
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

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // TODO: move stuff to an interface transformer
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
        return new InvokeDynamicInsnConverter(super.visitMethod(access, name, desc, signature, exceptions));
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


    private class InvokeDynamicInsnConverter extends MethodVisitor {

        public InvokeDynamicInsnConverter(MethodVisitor next) {
            super(ASM5, next);
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
            Class<?> invoker = loadClass(className);
            Handle implMethod = (Handle) bsmArgs[1];
            Handle bridgeMethod = getLambdaBridgeMethod(implMethod);

            LambdaFactoryMethod factory = LambdaReifier.reifyLambdaClass(implMethod, bridgeMethod,
                    invoker, invokedName, invokedType, bsm, bsmArgs);
            super.visitMethodInsn(INVOKESTATIC, factory.getOwner(), factory.getName(), factory.getDesc(), false);
        }
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
