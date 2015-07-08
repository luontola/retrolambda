// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.*;

import java.lang.invoke.MethodHandles;

import static org.objectweb.asm.Opcodes.ASM5;

public class RemoveMethodHandlesLookupReferences extends ClassVisitor {

    private static final String METHOD_HANDLES_LOOKUP = Type.getType(MethodHandles.Lookup.class).getInternalName();

    public RemoveMethodHandlesLookupReferences(ClassVisitor next) {
        super(ASM5, next);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (!name.equals(METHOD_HANDLES_LOOKUP)) {
            super.visitInnerClass(name, outerName, innerName, access);
        }
    }
}
