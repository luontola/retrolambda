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
    private Handle implMethod;
    private Handle accessMethod;

    public BackportLambdaClass(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        lambdaClass = name;
        implMethod = LambdaReifier.getLambdaImplMethod();
        accessMethod = LambdaReifier.getLambdaAccessMethod();

        if (superName.equals(LambdaNaming.MAGIC_LAMBDA_IMPL)) {
            superName = JAVA_LANG_OBJECT;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("<init>")) {
            Type constructor = Type.getMethodType(desc);
            int argumentCount = constructor.getArgumentTypes().length;

            String referenceName;
            String referenceDesc;
            if (argumentCount == 0) {
                // Add the static field and static initializer block to keep a singleton instance.
                makeSingleton();

                referenceName = SINGLETON_FIELD_NAME;
                referenceDesc = singletonFieldDesc();
            } else {
                // Make the constructor visible
                access &= ~ACC_PRIVATE;

                referenceName = "<init>";
                referenceDesc = desc;
            }

            LambdaClassInfo info = new LambdaClassInfo(lambdaClass, referenceName, referenceDesc, argumentCount);
            LambdaReifier.setClassInfo(info);
        }
        if (LambdaNaming.isSerializationHook(access, name, desc)) {
            return null; // remove serialization hooks; we serialize lambda instances as-is
        }
        if (LambdaNaming.isPlatformFactoryMethod(access, name)) {
            return null; // remove the JVM's factory method which will not be unused
        }
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        next = new RemoveMagicLambdaConstructorCall(next);
        next = new CallPrivateImplMethodsViaAccessMethods(next);
        return next;
    }

    private void makeSingleton() {
        FieldVisitor fv = super.visitField(ACC_STATIC | ACC_FINAL,
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

    private String singletonFieldDesc() {
        return "L" + lambdaClass + ";";
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
                    // the access method will also instantiate it.
                    // - The JVM would be OK with a non-empty stack on ARETURN, but it causes a VerifyError
                    //   on Android, so here we remove the unused instance from the stack.
                    // - We could improve this backporter so that it would remove the unnecessary
                    //   "NEW, DUP" instructions, but that would be complicated.
                    super.visitVarInsn(ASTORE, 1);
                    super.visitInsn(POP);
                    super.visitInsn(POP);
                    super.visitVarInsn(ALOAD, 1);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
