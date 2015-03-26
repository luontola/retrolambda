// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class UpdateRelocatedMethodInvocations extends ClassVisitor {

    private final ClassHierarchyAnalyzer analyzer;

    public UpdateRelocatedMethodInvocations(ClassVisitor next, ClassHierarchyAnalyzer analyzer) {
        super(ASM5, next);
        this.analyzer = analyzer;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new UpdateMethodCalls(super.visitMethod(access, name, desc, signature, exceptions));
    }

    private class UpdateMethodCalls extends MethodVisitor {

        public UpdateMethodCalls(MethodVisitor next) {
            super(Opcodes.ASM5, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            MethodRef method = new MethodRef(owner, name, desc);

            // change Interface.super.defaultMethod() calls to static calls on the companion class
            // TODO: move this inside getMethodCallTarget (also opcode, so must first change MethodRef to Handle)
            if (opcode == Opcodes.INVOKESPECIAL) {
                MethodRef impl = analyzer.getMethodDefaultImplementation(method);
                if (impl != null) {
                    opcode = Opcodes.INVOKESTATIC;
                    method = impl;
                }
            }

            method = analyzer.getMethodCallTarget(method);
            super.visitMethodInsn(opcode, method.owner, method.name, method.desc, itf);
        }
    }
}
