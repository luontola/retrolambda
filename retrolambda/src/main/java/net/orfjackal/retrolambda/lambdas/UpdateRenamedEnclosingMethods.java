// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import net.orfjackal.retrolambda.ClassAnalyzer;
import net.orfjackal.retrolambda.interfaces.MethodRef;
import org.objectweb.asm.ClassVisitor;

import static org.objectweb.asm.Opcodes.ASM5;

public class UpdateRenamedEnclosingMethods extends ClassVisitor {

    private final ClassAnalyzer analyzer;

    public UpdateRenamedEnclosingMethods(ClassVisitor next, ClassAnalyzer analyzer) {
        super(ASM5, next);
        this.analyzer = analyzer;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        MethodRef method = analyzer.getRenamedLambdaMethod(new MethodRef(0, owner, name, desc));
        super.visitOuterClass(method.owner, method.name, method.desc);
    }
}
