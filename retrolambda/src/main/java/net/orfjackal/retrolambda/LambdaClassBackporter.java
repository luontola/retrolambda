// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

public class LambdaClassBackporter {

    private static final String MAGIC_LAMBDA_IMPL = "java/lang/invoke/MagicLambdaImpl";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";

    public static byte[] transform(byte[] bytecode) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytecode).accept(new MyClassVisitor(writer), 0);
        return writer.toByteArray();
    }

    private static class MyClassVisitor extends ClassVisitor {

        public MyClassVisitor(ClassWriter cw) {
            super(Opcodes.ASM4, cw);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (superName.equals(MAGIC_LAMBDA_IMPL)) {
                superName = JAVA_LANG_OBJECT;
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (name.equals("<init>") && hasFlag(access, Opcodes.ACC_PRIVATE)) {
                access = clearFlag(access, Opcodes.ACC_PRIVATE); // make package-private (i.e. no flag)
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MagicLambdaRemovingMethodVisitor(mv);
        }
    }

    private static class MagicLambdaRemovingMethodVisitor extends MethodVisitor {

        public MagicLambdaRemovingMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (opcode == Opcodes.INVOKESPECIAL
                    && owner.equals(MAGIC_LAMBDA_IMPL)
                    && name.equals("<init>")
                    && desc.equals("()V")) {
                owner = JAVA_LANG_OBJECT;
            }
            super.visitMethodInsn(opcode, owner, name, desc);
        }
    }

    private static boolean hasFlag(int subject, int flag) {
        return (subject & flag) == flag;
    }

    private static int clearFlag(int subject, int flag) {
        return subject & ~flag;
    }
}
