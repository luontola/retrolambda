// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.util;

import net.orfjackal.retrolambda.lambdas.Handles;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class Bytecode {

    public static void generateDelegateMethod(ClassVisitor cv, int access, Handle method, Handle target) {
        MethodVisitor mv = cv.visitMethod(access, method.getName(), method.getDesc(), null, null);
        mv.visitCode();
        int varIndex = 0;
        for (Type type : Type.getArgumentTypes(method.getDesc())) {
            mv.visitVarInsn(type.getOpcode(ILOAD), varIndex);
            varIndex += type.getSize();
        }
        mv.visitMethodInsn(Handles.getOpcode(target), target.getOwner(), target.getName(), target.getDesc(), target.getTag() == H_INVOKEINTERFACE);
        mv.visitInsn(Type.getReturnType(method.getDesc()).getOpcode(IRETURN));
        mv.visitMaxs(-1, -1); // rely on ClassWriter.COMPUTE_MAXS
        mv.visitEnd();
    }
}
