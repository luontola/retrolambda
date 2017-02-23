// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.ClassAnalyzer;
import net.orfjackal.retrolambda.lambdas.Handles;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class UpdateRelocatedMethodInvocations extends ClassVisitor {

    private final ClassAnalyzer analyzer;

    public UpdateRelocatedMethodInvocations(ClassVisitor next, ClassAnalyzer analyzer) {
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
            MethodRef method = new MethodRef(Handles.opcodeToTag(opcode), owner, name, desc);
            method = analyzer.getMethodCallTarget(method);
            super.visitMethodInsn(method.getOpcode(), method.owner, method.name, method.desc, itf);
        }
    }
}
