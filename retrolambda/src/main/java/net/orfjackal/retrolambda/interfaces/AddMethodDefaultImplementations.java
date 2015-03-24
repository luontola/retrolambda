// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.Bytecode;
import org.objectweb.asm.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class AddMethodDefaultImplementations extends ClassVisitor {

    private final MethodRelocations methodRelocations;
    private String className;
    private String[] interfaces;
    private final Set<MethodSignature> methods = new HashSet<>();

    public AddMethodDefaultImplementations(ClassVisitor next, MethodRelocations methodRelocations) {
        super(ASM5, next);
        this.methodRelocations = methodRelocations;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.interfaces = interfaces;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        methods.add(new MethodSignature(name, desc));
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        for (String anInterface : interfaces) {
            for (MethodRef interfaceMethod : methodRelocations.getInterfaceMethods(Type.getObjectType(anInterface))) {
                if (!overrides(interfaceMethod.getSignature())) {
                    generateDefaultImplementation(interfaceMethod);
                }
            }
        }
        super.visitEnd();
    }

    private boolean overrides(MethodSignature method) {
        return thisOverrides(method) || superclassOverrides(method);
    }

    private boolean thisOverrides(MethodSignature method) {
        return methods.contains(method);
    }

    private boolean superclassOverrides(MethodSignature method) {
        for (MethodSignature superMethod : methodRelocations.getSuperclassMethods(Type.getObjectType(className))) {
            if (superMethod.equals(method)) {
                return true;
            }
        }
        return false;
    }

    private void generateDefaultImplementation(MethodRef interfaceMethod) {
        MethodRef impl = methodRelocations.getMethodDefaultImplementation(interfaceMethod);
        Bytecode.generateDelegateMethod(cv, ACC_PUBLIC | ACC_SYNTHETIC, interfaceMethod.toHandle(H_INVOKEVIRTUAL), impl.toHandle(H_INVOKESTATIC));
    }
}
