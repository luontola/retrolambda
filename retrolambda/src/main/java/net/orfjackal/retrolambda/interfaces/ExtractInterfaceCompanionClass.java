// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.Flags;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class ExtractInterfaceCompanionClass extends ClassVisitor {

    private final String companion;

    public ExtractInterfaceCompanionClass(ClassVisitor next, String companion) {
        super(ASM5, next);
        this.companion = companion;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        name = companion;
        access &= ~ACC_INTERFACE;
        access &= ~ACC_ABSTRACT;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (Flags.hasFlag(access, ACC_ABSTRACT)) {
            return null;
        }
        access |= ACC_STATIC;
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
