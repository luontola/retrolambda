// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public class BackportLambdaClass extends ClassVisitor {

    private static final String SINGLETON_FIELD_NAME = "instance";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";

    private String lambdaClass;
    private Type constructor;
    private Handle implMethod;
    private Handle accessMethod;
    private LambdaFactoryMethod factoryMethod;
    private EnclosingClass enclosingClass;
    private String sourceFile;
    private String sourceDebug;

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
        enclosingClass = LambdaReifier.getEnclosingClass();

        if (superName.equals(LambdaNaming.MAGIC_LAMBDA_IMPL)) {
            superName = JAVA_LANG_OBJECT;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        // This method will never be called if there is no debug information,
        // so we won't call super.visitSource() here but only in visitEnd().
        // (Probably this method is never called for any lambda, but never say never.)
        sourceFile = source;
        sourceDebug = debug;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("<init>")) {
            constructor = Type.getMethodType(desc);
        }
        if (LambdaNaming.isSerializationHook(access, name, desc)) {
            return null; // remove serialization hooks; we serialize lambda instances as-is
        }
        if (LambdaNaming.isPlatformFactoryMethod(access, name, desc, factoryMethod.getDesc())) {
            return null; // remove the JVM's factory method which will not be unused
        }
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        next = new RemoveLambdaFormHiddenAnnotation(next);
        next = new RemoveMagicLambdaConstructorCall(next);
        next = new CallPrivateImplMethodsViaAccessMethods(access, name, desc, signature, exceptions, next);
        return next;
    }

    @Override
    public void visitEnd() {
        if (isStateless()) {
            makeSingleton();
        }
        generateFactoryMethod();
        if (sourceFile == null) {
            sourceFile = enclosingClass.sourceFile;
        }
        super.visitSource(sourceFile, sourceDebug);
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

    private class CallPrivateImplMethodsViaAccessMethods extends MethodNode {
        private final MethodVisitor next;

        public CallPrivateImplMethodsViaAccessMethods(int access, String name, String desc, String signature,
                                                      String[] exceptions, MethodVisitor next) {
            super(ASM5, access, name, desc, signature, exceptions);
            this.next = next;
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

                if (implMethod.getTag() == H_NEWINVOKESPECIAL
                        && accessMethod.getTag() == H_INVOKESTATIC) {
                    // The impl is a private constructor which is called through an access method.
                    // The current method already did NEW an instance, but we won't use it because
                    // the access method will also instantiate it. The JVM would be OK with a non-empty
                    // stack on ARETURN, but it causes a VerifyError on Android, so here we remove the
                    // unused instance from the stack.
                    boolean found = false;
                    for (int i = instructions.size() - 1; i >= 1; i--) {
                        AbstractInsnNode maybeNew = instructions.get(i - 1);
                        AbstractInsnNode maybeDup = instructions.get(i);
                        if (maybeNew.getOpcode() == NEW && maybeDup.getOpcode() == DUP) {
                            instructions.remove(maybeNew);
                            instructions.remove(maybeDup);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new IllegalStateException(
                                "Expected to find NEW, DUP instructions preceding NEWINVOKESPECIAL. Please file this as a bug.");
                    }
                }

                super.visitMethodInsn(
                        Handles.getOpcode(accessMethod),
                        accessMethod.getOwner(),
                        accessMethod.getName(),
                        accessMethod.getDesc(),
                        accessMethod.getTag() == H_INVOKEINTERFACE);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

        @Override
        public void visitEnd() {
            accept(next);
        }
    }
}
