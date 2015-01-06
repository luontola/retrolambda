// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.defaultmethods.*;
import net.orfjackal.retrolambda.interfaces.*;
import net.orfjackal.retrolambda.lambdas.*;
import net.orfjackal.retrolambda.trywithresources.SwallowSuppressedExceptions;
import org.objectweb.asm.*;

public class Transformers {

    private final int targetVersion;
    private final MethodRelocations methodRelocations;

    public Transformers(int targetVersion, MethodRelocations methodRelocations) {
        this.targetVersion = targetVersion;
        this.methodRelocations = methodRelocations;
    }

    public byte[] backportLambdaClass(ClassReader reader) {
        return transform(reader, (next) -> {
            if (FeatureToggles.DEFAULT_METHODS == 1) {
                next = new ClassModifier(targetVersion, next);
            } else if (FeatureToggles.DEFAULT_METHODS == 2) {
                next = new UpdateRelocatedMethodInvocations(next, methodRelocations);
                next = new AddMethodDefaultImplementations(next, methodRelocations);
            } else {
                next = new UpdateRelocatedMethodInvocations(next, methodRelocations); // needed for lambdas in an interface's constant initializer
            }
            next = new BackportLambdaClass(next);
            return next;
        });
    }

    public byte[] backportClass(ClassReader reader) {
        return transform(reader, (next) -> {
            if (FeatureToggles.DEFAULT_METHODS == 1) {
                next = new ClassModifier(targetVersion, next);
                next = new InterfaceModifier(next, targetVersion);
            } else if (FeatureToggles.DEFAULT_METHODS == 2) {
                next = new UpdateRelocatedMethodInvocations(next, methodRelocations);
                next = new AddMethodDefaultImplementations(next, methodRelocations);
            }
            next = new BackportLambdaInvocations(next);
            return next;
        });
    }

    public byte[] backportInterface(ClassReader reader) {
        return transform(reader, (next) -> {
            if (FeatureToggles.DEFAULT_METHODS == 1) {
                next = new ClassModifier(targetVersion, next);
                next = new InterfaceModifier(next, targetVersion);
            } else if (FeatureToggles.DEFAULT_METHODS == 2) {
                next = new RemoveStaticMethods(next);
                next = new RemoveDefaultMethodBodies(next);
                next = new UpdateRelocatedMethodInvocations(next, methodRelocations);
            } else {
                next = new RemoveStaticMethods(next); // needed for lambdas in an interface's constant initializer
                next = new WarnAboutDefaultAndStaticMethods(next);
            }
            next = new RemoveBridgeMethods(next);
            next = new BackportLambdaInvocations(next);
            return next;
        });
    }

    public byte[] extractInterfaceCompanion(ClassReader reader) {
        String companion = methodRelocations.getCompanionClass(reader.getClassName());
        if (companion == null) {
            return null;
        }
        return transform(reader, (next) -> {
            next = new UpdateRelocatedMethodInvocations(next, methodRelocations);
            next = new ExtractInterfaceCompanionClass(next, companion);
            return next;
        });
    }

    private byte[] transform(ClassReader reader, ClassVisitorChain chain) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor next = writer;

        next = new LowerBytecodeVersion(next, targetVersion);
        if (targetVersion < Opcodes.V1_7) {
            next = new SwallowSuppressedExceptions(next);
        }
        next = new FixInvokeStaticOnInterfaceMethod(next);
        next = chain.wrap(next);

        reader.accept(next, 0);
        return writer.toByteArray();
    }

    private interface ClassVisitorChain {
        ClassVisitor wrap(ClassVisitor next);
    }
}
