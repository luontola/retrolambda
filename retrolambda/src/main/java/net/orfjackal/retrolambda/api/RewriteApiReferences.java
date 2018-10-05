// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.api;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * Rewrites calls new Java APIs to backported classes
 */
public class RewriteApiReferences extends ClassRemapper {

    private final ApiMappingSet mapping;

    public RewriteApiReferences(ClassVisitor next, ApiMappingSet mapping) {
        super(Opcodes.ASM5, next, new ApiRemapper(mapping));
        this.mapping = mapping;
    }

    @Override
    protected MethodVisitor createMethodRemapper(MethodVisitor mv) {
        return new MethodRemapper(mv, remapper) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if(opcode == Opcodes.GETSTATIC) {
                    Mapping fieldMapping = mapping.mapField(owner, name, desc);
                    super.visitFieldInsn(opcode, fieldMapping.getOwner(), fieldMapping.getName(), desc);
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if(opcode == Opcodes.INVOKESTATIC) {
                    Mapping methodMapping = mapping.mapStaticMethod(owner, name, desc);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                            methodMapping.getOwner(),
                            methodMapping.getName(),
                            remapper.mapDesc(desc), itf);
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        };
    }
}
