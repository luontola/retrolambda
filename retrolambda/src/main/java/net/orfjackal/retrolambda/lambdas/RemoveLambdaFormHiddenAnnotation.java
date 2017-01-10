// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.*;

public class RemoveLambdaFormHiddenAnnotation extends MethodVisitor {

    private static final String LAMBDA_FORM_HIDDEN_NAME = "Ljava/lang/invoke/LambdaForm$Hidden;";

    public RemoveLambdaFormHiddenAnnotation(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (LAMBDA_FORM_HIDDEN_NAME.equals(desc)) {
            return null;
        }
        return super.visitAnnotation(desc, visible);
    }
}
