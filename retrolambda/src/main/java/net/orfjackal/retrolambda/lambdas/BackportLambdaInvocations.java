// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import net.orfjackal.retrolambda.util.Bytecode;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

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
        if (LambdaNaming.isDeserializationHook(access, name, desc)) {
            return null; // remove serialization hooks; we serialize lambda instances as-is
        }
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        return new InvokeDynamicInsnConverter(access, name, desc, signature, exceptions, next);
    }

    Handle getLambdaAccessMethod(Handle implMethod) {
        if (!implMethod.getOwner().equals(className)) {
            return implMethod;
        }
        if (isInterface(classAccess)) {
            // the method will be relocated to a companion class
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


    private class InvokeDynamicInsnConverter extends MethodNode {
        private final MethodVisitor next;

        public InvokeDynamicInsnConverter(int access, String name, String desc, String signature, String[] exceptions,
                                          MethodVisitor next) {
            super(ASM5, access, name, desc, signature, exceptions);
            this.next = next;
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

            LambdaClassInfo info = LambdaReifier.reifyLambdaClass(implMethod, accessMethod,
                    invoker, invokedName, invokedType, bsm, bsmArgs);

            if (info.isStateless()) {
                super.visitFieldInsn(GETSTATIC, info.getLambdaClass(), info.getReferenceName(),
                        info.getReferenceDesc());
                return;
            }

            // At this point we know that the lambda is capturing and will require load bytecodes for its arguments.
            // Since these must occur after the new/dup bytecodes, find the first load instruction and place the
            // new/dup bytecode before it.
            AbstractInsnNode firstLoad = null;
            for (int i = instructions.size() - 1, seen = 0; i >= 0; i--) {
                AbstractInsnNode instruction = instructions.get(i);
                int opcode = instruction.getOpcode();
                if (opcode == ALOAD || opcode == ILOAD || opcode == LLOAD || opcode == FLOAD || opcode == DLOAD || opcode == GETSTATIC) {
                    seen++;
                    if (seen == info.getArgumentCount()) {
                        firstLoad = instruction;
                        break;
                    }
                }
            }
            if (firstLoad == null) {
                throw new IllegalStateException(
                        "Unable to find expected load instruction count. Please report this as a bug.");
            }

            instructions.insertBefore(firstLoad, new TypeInsnNode(NEW, info.getLambdaClass()));
            instructions.insertBefore(firstLoad, new InsnNode(DUP));

            super.visitMethodInsn(INVOKESPECIAL, info.getLambdaClass(), info.getReferenceName(),
                    info.getReferenceDesc(), false);
        }

        @Override
        public void visitEnd() {
            // Forward all recorded instructions to the delegate method visitor.
            accept(next);
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
