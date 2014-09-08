// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class BackportLambdaClass extends ClassVisitor {

    private static final String SINGLETON_FIELD_NAME = "instance";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";

    private String lambdaClass;
    private Type constructor;
    private Handle implMethod;
    private Handle bridgeMethod;
    private LambdaFactoryMethod factoryMethod;

    public BackportLambdaClass(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        lambdaClass = name;
        LambdaReifier.setLambdaClass(lambdaClass);
        implMethod = LambdaReifier.getLambdaImplMethod();
        bridgeMethod = LambdaReifier.getLambdaBridgeMethod();
        factoryMethod = LambdaReifier.getLambdaFactoryMethod();

        if (superName.equals(LambdaNaming.MAGIC_LAMBDA_IMPL)) {
            superName = JAVA_LANG_OBJECT;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("<init>")) {
            constructor = Type.getMethodType(desc);
        }
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        next = new RemoveMagicLambdaConstructorCall(next);
        next = new BridgePrivateMethodInvocations(next);
        return next;
    }

    @Override
    public void visitEnd() {
        if (isStateless()) {
            makeSingleton();
        }
        generateFactoryMethod();
        super.visitEnd();
    }

    private void makeSingleton() {
        FieldVisitor fv = super.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                SINGLETON_FIELD_NAME, singletonFieldDesc(), null, null);
        fv.visitEnd();

        MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, lambdaClass);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, lambdaClass, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, lambdaClass, SINGLETON_FIELD_NAME, singletonFieldDesc());
        mv.visitInsn(RETURN);
        mv.visitMaxs(-1, -1); // rely on ClassWriter.COMPUTE_MAXS
        mv.visitEnd();
    }

    private void generateFactoryMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC | ACC_STATIC,
                factoryMethod.getName(), factoryMethod.getDesc(), null, null);
        mv.visitCode();

        if (isStateless()) {
            mv.visitFieldInsn(GETSTATIC, lambdaClass, SINGLETON_FIELD_NAME, singletonFieldDesc());
            mv.visitInsn(ARETURN);

        } else {
            mv.visitTypeInsn(NEW, lambdaClass);
            mv.visitInsn(DUP);
            int varIndex = 0;
            for (Type type : constructor.getArgumentTypes()) {
                mv.visitVarInsn(type.getOpcode(ILOAD), varIndex);
                varIndex += type.getSize();
            }
            mv.visitMethodInsn(INVOKESPECIAL, lambdaClass, "<init>", constructor.getDescriptor(), false);
            mv.visitInsn(ARETURN);
        }

        mv.visitMaxs(-1, -1); // rely on ClassWriter.COMPUTE_MAXS
        mv.visitEnd();
    }

    private String singletonFieldDesc() {
        return "L" + lambdaClass + ";";
    }

    private boolean isStateless() {
        return constructor.getArgumentTypes().length == 0;
    }


    private static class RemoveMagicLambdaConstructorCall extends MethodVisitor {

        public RemoveMagicLambdaConstructorCall(MethodVisitor next) {
            super(ASM5, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKESPECIAL
                    && owner.equals(LambdaNaming.MAGIC_LAMBDA_IMPL)
                    && name.equals("<init>")
                    && desc.equals("()V")) {
                owner = JAVA_LANG_OBJECT;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private class BridgePrivateMethodInvocations extends MethodVisitor {

        public BridgePrivateMethodInvocations(MethodVisitor next) {
            super(ASM5, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            // Java 8's lambda classes get away with calling private virtual methods
            // by using invokespecial because the JVM relaxes the bytecode validation
            // of the lambda classes it generates. We must however call them through
            // a non-private bridge method which we have generated.
            if (owner.equals(implMethod.getOwner())
                    && name.equals(implMethod.getName())
                    && desc.equals(implMethod.getDesc())) {
                super.visitMethodInsn(
                        Handles.getOpcode(bridgeMethod),
                        bridgeMethod.getOwner(),
                        bridgeMethod.getName(),
                        bridgeMethod.getDesc(),
                        bridgeMethod.getTag() == H_INVOKEINTERFACE);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
