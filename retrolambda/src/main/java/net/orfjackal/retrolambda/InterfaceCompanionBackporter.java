// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

public class InterfaceCompanionBackporter {

    public static byte[] transform(ClassReader reader, int targetVersion, MethodRelocations methodRelocations) {
        String companion;
        if (FeatureToggles.DEFAULT_METHODS == 2
                && isInterface(reader)
                && (companion = methodRelocations.getCompanionClass(reader.getClassName())) != null) {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor next = writer;
            next = new ApplyMethodRelocations(next, methodRelocations);
            next = new ExtractInterfaceCompanionClass(next, companion);
            next = new InvokeStaticInterfaceMethodConverter(next);
            next = new LowerBytecodeVersion(next, targetVersion);
            reader.accept(next, 0);
            return writer.toByteArray();
        } else {
            return null;
        }
    }

    private static boolean isInterface(ClassReader reader) {
        return Flags.hasFlag(reader.getAccess(), Opcodes.ACC_INTERFACE);
    }
}
