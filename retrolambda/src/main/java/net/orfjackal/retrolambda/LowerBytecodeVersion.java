// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class LowerBytecodeVersion extends ClassVisitor {

    private final int targetVersion;

    public LowerBytecodeVersion(ClassVisitor next, int targetVersion) {
        super(ASM5, next);
        this.targetVersion = targetVersion;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (version > targetVersion) {
            version = targetVersion;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        if (targetVersion <= Opcodes.V1_5) {
            return new RemoveMethodFrames(next);
        } else {
            return next;
        }
    }

    private static class RemoveMethodFrames extends MethodVisitor {

        public RemoveMethodFrames(MethodVisitor next) {
            super(Opcodes.ASM5, next);
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            // remove frame
        }
    }
}
