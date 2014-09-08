// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.defaultmethods.*;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class LambdaUsageBackporter {

    public static byte[] transform(ClassReader reader, int targetVersion, MethodRelocations methodRelocations) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor next = writer;
        if (FeatureToggles.DEFAULT_METHODS == 1) {
            next = new ClassModifier(targetVersion, next);
            next = new InterfaceModifier(next, targetVersion);
        } else if (FeatureToggles.DEFAULT_METHODS == 2) {
            if (isInterface(reader)) {
                next = new RemoveStaticMethods(next);
                next = new RemoveDefaultMethodBodies(next);
            }
            next = new ApplyMethodRelocations(next, methodRelocations);
            next = new InvokeStaticInterfaceMethodConverter(next);
        } else {
            next = new InvokeStaticInterfaceMethodConverter(next);
        }
        next = new BackportLambdaInvocations(next);
        next = new LowerBytecodeVersion(next, targetVersion);
        reader.accept(next, 0);
        return writer.toByteArray();
    }

    private static boolean isInterface(ClassReader reader) {
        return Flags.hasFlag(reader.getAccess(), ACC_INTERFACE);
    }
}
