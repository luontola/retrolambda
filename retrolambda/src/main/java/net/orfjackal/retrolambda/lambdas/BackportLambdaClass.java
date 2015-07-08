// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
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
    private Handle accessMethod;
    private LambdaFactoryMethod factoryMethod;

    public BackportLambdaClass(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        lambdaClass = name;
        LambdaReifier.setLambdaClass(lambdaClass);
        implMethod = LambdaReifier.getLambdaImplMethod();
        accessMethod = LambdaReifier.getLambdaAccessMethod();
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
        if (LambdaNaming.isSerializationHook(access, name, desc)) {
            return null; // remove serialization hooks; we serialize lambda instances as-is
        }
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        next = new RemoveMagicLambdaConstructorCall(next);
        next = new CallPrivateImplMethodsViaAccessMethods(next);
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

    private class CallPrivateImplMethodsViaAccessMethods extends MethodVisitor {

        public CallPrivateImplMethodsViaAccessMethods(MethodVisitor next) {
            super(ASM5, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            // Java 8's lambda classes get away with calling private virtual methods
            // by using invokespecial because the JVM relaxes the bytecode validation
            // of the lambda classes it generates. We must however call them through
            // a non-private access method which we have generated.
            if (owner.equals(implMethod.getOwner())
                    && name.equals(implMethod.getName())
                    && desc.equals(implMethod.getDesc())) {
                super.visitMethodInsn(
                        Handles.getOpcode(accessMethod),
                        accessMethod.getOwner(),
                        accessMethod.getName(),
                        accessMethod.getDesc(),
                        accessMethod.getTag() == H_INVOKEINTERFACE);

                if (implMethod.getTag() == H_NEWINVOKESPECIAL
                        && accessMethod.getTag() == H_INVOKESTATIC) {
                    // The impl is a private constructor which is called through an access method.
                    // XXX: The current method already did NEW an instance, but we won't use it because
                    // the access method will also instantiate it, so we could remove the unused
                    // instance from stack using the following code, but this is not strictly necessary
                    // because ARETURN is allowed to leave behind a non-empty stack. We could improve
                    // this backporter so that it would remove the unnecessary "NEW, DUP" instructions,
                    // but that would be complicated.
                    if (false) {
                        super.visitVarInsn(ASTORE, 1);
                        super.visitInsn(POP);
                        super.visitInsn(POP);
                        super.visitVarInsn(ALOAD, 1);
                    }
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
