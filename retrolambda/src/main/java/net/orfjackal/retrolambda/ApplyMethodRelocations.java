// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import com.google.common.base.Preconditions;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class ApplyMethodRelocations extends ClassVisitor {

    private final MethodRelocations methodRelocations;

    public ApplyMethodRelocations(ClassVisitor next, MethodRelocations methodRelocations) {
        super(ASM5, next);
        this.methodRelocations = methodRelocations;
        Preconditions.checkNotNull(methodRelocations);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new ApplyRenamesMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    private class ApplyRenamesMethodVisitor extends MethodVisitor {

        public ApplyRenamesMethodVisitor(MethodVisitor next) {
            super(Opcodes.ASM5, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            MethodRef ref = methodRelocations.getMethodLocation(new MethodRef(owner, name, desc));
            super.visitMethodInsn(opcode, ref.owner, ref.name, ref.desc, itf);
        }
    }
}
