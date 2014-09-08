// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class FixInvokeStaticOnInterfaceMethod extends ClassVisitor {

    public FixInvokeStaticOnInterfaceMethod(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }


    private static class MyMethodVisitor extends MethodVisitor {

        public MyMethodVisitor(MethodVisitor next) {
            super(Opcodes.ASM5, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKESTATIC && itf) {
                // pre-Java8 bytecode is not allowed to do invokestatic calls on interface method references
                itf = false;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
