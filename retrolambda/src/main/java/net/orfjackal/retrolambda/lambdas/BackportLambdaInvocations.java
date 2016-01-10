// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import net.orfjackal.retrolambda.util.Bytecode;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.orfjackal.retrolambda.util.Flags.isInterface;
import static org.objectweb.asm.Opcodes.*;

public class BackportLambdaInvocations extends ClassVisitor {

    private int classAccess;
    private String className;
    private final Map<Handle, Handle> lambdaAccessToImplMethods = new LinkedHashMap<>();

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
        if (LambdaNaming.isBodyMethod(access, name)) {
            // Ensure our generated lambda class is able to call this method.
            access &= ~ACC_PRIVATE;
        }
        if (LambdaNaming.isDeserializationHook(access, name, desc)) {
            return null; // remove serialization hooks; we serialize lambda instances as-is
        }
        return new InvokeDynamicInsnConverter(super.visitMethod(access, name, desc, signature, exceptions));
    }

    Handle getLambdaAccessMethod(Handle implMethod) {
        if (!implMethod.getOwner().equals(className)) {
            return implMethod;
        }
        if (isInterface(classAccess)) {
            // the method will be relocated to a companion class
            return implMethod;
        }
        if (LambdaNaming.isBodyMethodName(implMethod.getName())) {
            if (implMethod.getTag() == H_INVOKESPECIAL) {
                // The private body method is now package so switch its invocation from special to virtual.
                return new Handle(H_INVOKEVIRTUAL, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc());
            }
            return implMethod;
        }
        // TODO: do not generate an access method if the impl method is not private (probably not implementable with a single pass)
        String name = "access$lambda$" + lambdaAccessToImplMethods.size();
        String desc = getLambdaAccessMethodDesc(implMethod);
        Handle accessMethod = new Handle(H_INVOKESTATIC, className, name, desc);
        lambdaAccessToImplMethods.put(accessMethod, implMethod);
        return accessMethod;
    }

    private String getLambdaAccessMethodDesc(Handle implMethod) {
        if (implMethod.getTag() == H_INVOKESTATIC) {
            // static method call -> keep as-is
            return implMethod.getDesc();

        } else if (implMethod.getTag() == H_NEWINVOKESPECIAL) {
            // constructor call -> change to a a factory method
            return Types.changeReturnType(Type.getObjectType(implMethod.getOwner()), implMethod.getDesc());

        } else {
            // instance method call -> change to a static method
            return Types.prependArgumentType(Type.getObjectType(className), implMethod.getDesc());
        }
    }

    @Override
    public void visitEnd() {
        for (Map.Entry<Handle, Handle> entry : lambdaAccessToImplMethods.entrySet()) {
            Handle accessMethod = entry.getKey();
            Handle implMethod = entry.getValue();
            Bytecode.generateDelegateMethod(cv, ACC_STATIC | ACC_SYNTHETIC, accessMethod, implMethod);
        }
        super.visitEnd();
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
            Handle accessMethod = getLambdaAccessMethod(implMethod);

            LambdaFactoryMethod factory = LambdaReifier.reifyLambdaClass(implMethod, accessMethod,
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
