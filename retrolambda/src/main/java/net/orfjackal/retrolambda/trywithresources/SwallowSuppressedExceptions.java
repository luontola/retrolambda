// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.trywithresources;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class SwallowSuppressedExceptions extends ClassVisitor {

    public SwallowSuppressedExceptions(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodVisitor(ASM5, next) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEVIRTUAL
                        && name.equals("addSuppressed")
                        && desc.equals("(Ljava/lang/Throwable;)V")
                        && (owner.equals("java/lang/Throwable")
                                || owner.endsWith("Exception")
                                || owner.endsWith("Error"))) {
                    super.visitInsn(Opcodes.POP); // the suppressed exception
                    super.visitInsn(Opcodes.POP); // the original exception
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        };
    }
}
