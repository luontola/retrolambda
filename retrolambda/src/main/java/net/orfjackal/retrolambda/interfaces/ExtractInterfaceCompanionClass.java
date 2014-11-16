// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.*;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class ExtractInterfaceCompanionClass extends ClassVisitor {

    private final String companion;
    private String interfaceName;

    public ExtractInterfaceCompanionClass(ClassVisitor next, String companion) {
        super(ASM5, next);
        this.companion = companion;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        interfaceName = name;
        name = companion;
        access &= ~ACC_INTERFACE;
        access &= ~ACC_ABSTRACT;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (Flags.hasFlag(access, ACC_ABSTRACT)) {
            // do not copy abstract methods to the companion class
            return null;
        }
        if (!Flags.hasFlag(access, ACC_STATIC)) {
            // default method; make static and take 'this' as the first argument
            access |= ACC_STATIC;
            // TODO: this adding of the first argument is duplicated in ClassHierarchyAnalyzer
            desc = Bytecode.prependArgumentType(desc, Type.getObjectType(interfaceName));
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
