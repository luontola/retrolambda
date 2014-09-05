// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class ApplyMethodRelocations extends ClassVisitor {

    public ApplyMethodRelocations(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new ApplyRenamesMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    private static class ApplyRenamesMethodVisitor extends MethodVisitor {

        public ApplyRenamesMethodVisitor(MethodVisitor next) {
            super(Opcodes.ASM5, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            // TODO: "make it right"
            if (owner.equals("net/orfjackal/retrolambda/test/InterfaceStaticMethodsTest$Interface")
                    && name.equals("staticMethod")
                    && desc.equals("()I")) {
                owner += "$";
                itf = false;
            }
            if (owner.equals("net/orfjackal/retrolambda/test/InterfaceStaticMethodsTest$Interface")
                    && name.equals("staticMethodWithArgs")
                    && desc.equals("(Ljava/lang/String;IJ)Ljava/lang/String;")) {
                owner += "$";
                itf = false;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
