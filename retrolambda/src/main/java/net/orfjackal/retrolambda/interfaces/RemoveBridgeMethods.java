// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.Flags;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class RemoveBridgeMethods extends ClassVisitor {

    public RemoveBridgeMethods(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (Flags.hasFlag(access, ACC_BRIDGE)) {
            return null; // remove the bridge method; Java 7 didn't use them
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
