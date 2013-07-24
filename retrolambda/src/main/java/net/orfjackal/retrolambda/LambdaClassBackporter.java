// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class LambdaClassBackporter {

    private static final String MAGIC_LAMBDA_IMPL = "java/lang/invoke/MagicLambdaImpl";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";

    public static byte[] transform(byte[] bytecode, int targetVersion) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytecode).accept(new MyClassVisitor(writer, targetVersion), 0);
        return writer.toByteArray();
    }

    private static class MyClassVisitor extends ClassVisitor {
        private final int targetVersion;
        private String className;
        private boolean stateless = false;

        public MyClassVisitor(ClassWriter cw, int targetVersion) {
            super(ASM4, cw);
            this.targetVersion = targetVersion;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = name;
            if (version > targetVersion) {
                version = targetVersion;
            }
            if (superName.equals(MAGIC_LAMBDA_IMPL)) {
                superName = JAVA_LANG_OBJECT;
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (name.equals("<init>")) {
                access = Flags.makeNonPrivate(access);
            }
            if (name.equals("<init>") && desc.equals("()V")) {
                stateless = true;
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MagicLambdaRemovingMethodVisitor(mv);
        }

        @Override
        public void visitEnd() {
            if (stateless) {
                makeSingleton();
            }
            super.visitEnd();
        }

        private void makeSingleton() {
            String fieldName = "instance";
            String fieldDesc = "L" + className + ";";

            FieldVisitor fv = super.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, fieldName, fieldDesc, null, null);
            fv.visitEnd();

            MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, className);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V");
            mv.visitFieldInsn(PUTSTATIC, className, fieldName, fieldDesc);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
    }

    private static class MagicLambdaRemovingMethodVisitor extends MethodVisitor {

        public MagicLambdaRemovingMethodVisitor(MethodVisitor mv) {
            super(ASM4, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (opcode == INVOKESPECIAL
                    && owner.equals(MAGIC_LAMBDA_IMPL)
                    && name.equals("<init>")
                    && desc.equals("()V")) {
                owner = JAVA_LANG_OBJECT;
            }
            super.visitMethodInsn(opcode, owner, name, desc);
        }
    }
}
