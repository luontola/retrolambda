// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.interfaces.*;
import net.orfjackal.retrolambda.lambdas.*;
import net.orfjackal.retrolambda.trywithresources.SwallowSuppressedExceptions;
import org.objectweb.asm.*;

import java.util.Optional;

public class Transformers {

    private final int targetVersion;
    private final boolean defaultMethodsEnabled;
    private final ClassHierarchyAnalyzer analyzer;

    public Transformers(int targetVersion, boolean defaultMethodsEnabled, ClassHierarchyAnalyzer analyzer) {
        this.targetVersion = targetVersion;
        this.defaultMethodsEnabled = defaultMethodsEnabled;
        this.analyzer = analyzer;
    }

    public byte[] backportLambdaClass(ClassReader reader) {
        return transform(reader, (next) -> {
            if (defaultMethodsEnabled) {
                // Lambda classes are generated dynamically, so they were not
                // part of the original analytics and must be analyzed now,
                // in case they implement interfaces with default methods.
                analyzer.analyze(reader);
                next = new UpdateRelocatedMethodInvocations(next, analyzer);
                next = new AddMethodDefaultImplementations(next, analyzer);
            } else {
                next = new UpdateRelocatedMethodInvocations(next, analyzer); // needed for lambdas in an interface's constant initializer
            }
            next = new BackportLambdaClass(next);
            return next;
        });
    }

    public byte[] backportClass(ClassReader reader) {
        return transform(reader, (next) -> {
            if (defaultMethodsEnabled) {
                next = new UpdateRelocatedMethodInvocations(next, analyzer);
                next = new AddMethodDefaultImplementations(next, analyzer);
            }
            next = new BackportLambdaInvocations(next);
            return next;
        });
    }

    public byte[] backportInterface(ClassReader reader) {
        return transform(reader, (next) -> {
            if (defaultMethodsEnabled) {
                next = new RemoveStaticMethods(next);
                next = new RemoveDefaultMethodBodies(next);
                next = new UpdateRelocatedMethodInvocations(next, analyzer);
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
        Optional<Type> companion = analyzer.getCompanionClass(Type.getObjectType(reader.getClassName()));
        if (!companion.isPresent()) {
            return null;
        }
        return transform(reader, (next) -> {
            next = new UpdateRelocatedMethodInvocations(next, analyzer);
            next = new ExtractInterfaceCompanionClass(next, companion.get());
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
