// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.defaultmethods.ClassModifier;
import org.objectweb.asm.*;

public class LambdaClassBackporter {

    public static byte[] transform(byte[] bytecode, int targetVersion, MethodRelocations methodRelocations) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor next = writer;
        if (FeatureToggles.DEFAULT_METHODS == 1) {
            next = new ClassModifier(targetVersion, next);
        } else if (FeatureToggles.DEFAULT_METHODS == 2) {
            next = new ApplyMethodRelocations(next, methodRelocations);
        }
        next = new BackportLambdaClass(next);
        next = new LowerBytecodeVersion(next, targetVersion);
        new ClassReader(bytecode).accept(next, 0);
        return writer.toByteArray();
    }
}
