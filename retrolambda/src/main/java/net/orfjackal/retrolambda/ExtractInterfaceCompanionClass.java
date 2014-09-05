// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.ClassVisitor;

import static org.objectweb.asm.Opcodes.*;

public class ExtractInterfaceCompanionClass extends ClassVisitor {

    public ExtractInterfaceCompanionClass(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // TODO: "make it right"
        if (name.equals("net/orfjackal/retrolambda/test/InterfaceStaticMethodsTest$Interface")) {
            name += "$";
            access &= ~ACC_INTERFACE;
            access &= ~ACC_ABSTRACT;
        }
        // TODO: remove abstract methods
        super.visit(version, access, name, signature, superName, interfaces);
    }
}
