// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.Bytecode;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class AddMethodDefaultImplementations extends ClassVisitor {

    private final MethodRelocations methodRelocations;
    private String className;

    public AddMethodDefaultImplementations(ClassVisitor next, MethodRelocations methodRelocations) {
        super(ASM5, next);
        this.methodRelocations = methodRelocations;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        for (MethodInfo method : methodRelocations.getDefaultMethods(Type.getObjectType(className))) {
            MethodRef interfaceMethod = method.toMethodRef();
            MethodRef defaultImpl = ((MethodKind.Default) method.kind).defaultImpl;
            Bytecode.generateDelegateMethod(cv, ACC_PUBLIC | ACC_SYNTHETIC, interfaceMethod.toHandle(H_INVOKEVIRTUAL), defaultImpl.toHandle(H_INVOKESTATIC));
        }
        super.visitEnd();
    }
}
