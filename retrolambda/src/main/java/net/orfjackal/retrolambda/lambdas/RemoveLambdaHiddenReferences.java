// Copyright © 2013-2016 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class RemoveLambdaHiddenReferences extends ClassVisitor {

    private static final String LAMBDA_FORM_HIDDEN_NAME = "Ljava/lang/invoke/LambdaForm$Hidden;";

    public RemoveLambdaHiddenReferences(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodVisitor(ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (LAMBDA_FORM_HIDDEN_NAME.equals(desc)) {
                    return null;
                }
                return super.visitAnnotation(desc, visible);
            }
        };
    }
}
