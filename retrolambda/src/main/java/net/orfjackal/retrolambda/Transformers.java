// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.interfaces.*;
import net.orfjackal.retrolambda.lambdas.*;
import net.orfjackal.retrolambda.requirenonnull.RequireNonNull;
import net.orfjackal.retrolambda.trywithresources.SwallowSuppressedExceptions;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.function.Consumer;

public class Transformers {

    private final int targetVersion;
    private final boolean defaultMethodsEnabled;
    private final ClassAnalyzer analyzer;

    public Transformers(int targetVersion, boolean defaultMethodsEnabled, ClassAnalyzer analyzer) {
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
            next = new BackportLambdaInvocations(next, analyzer);
            return next;
        });
    }

    public List<byte[]> backportInterface(ClassReader reader) {
        // The lambdas must be backported only once, because bad things will happen if a lambda
        // is called by different class name in the interface and its companion class, and then
        // the wrong one of them is written to disk last.
        ClassNode lambdasBackported = new ClassNode();
        ClassVisitor next = lambdasBackported;
        next = new BackportLambdaInvocations(next, analyzer);
        reader.accept(next, 0);

        List<byte[]> results = new ArrayList<>();
        results.add(backportInterface2(lambdasBackported));
        results.addAll(extractInterfaceCompanion(lambdasBackported));
        return results;
    }

    private byte[] backportInterface2(ClassNode clazz) {
        return transform(clazz, (next) -> {
            if (defaultMethodsEnabled) {
                next = new RemoveStaticMethods(next);
                next = new RemoveDefaultMethodBodies(next);
                next = new UpdateRelocatedMethodInvocations(next, analyzer);
            } else {
                // XXX: It would be better to remove only those static methods which are lambda implementation methods,
                // but that would either require the use of naming patterns (not guaranteed to work with every Java compiler)
                // or passing around information that which relocated static methods are because of lambdas.
                next = new RemoveStaticMethods(next); // needed for lambdas in an interface's constant initializer
                next = new WarnAboutDefaultAndStaticMethods(next);
            }
            next = new RemoveBridgeMethods(next);
            return next;
        });
    }

    private List<byte[]> extractInterfaceCompanion(ClassNode clazz) {
        Optional<Type> companion = analyzer.getCompanionClass(Type.getObjectType(clazz.name));
        if (!companion.isPresent()) {
            return Collections.emptyList();
        }
        return Arrays.asList(transform(clazz, (next) -> {
            next = new UpdateRelocatedMethodInvocations(next, analyzer);
            next = new ExtractInterfaceCompanionClass(next, companion.get());
            return next;
        }));
    }

    private byte[] transform(ClassNode node, ClassVisitorChain chain) {
        return transform(node.name, node::accept, chain);
    }

    private byte[] transform(ClassReader reader, ClassVisitorChain chain) {
        return transform(reader.getClassName(), cv -> reader.accept(cv, 0), chain);
    }

    private byte[] transform(String className, Consumer<ClassVisitor> reader, ClassVisitorChain chain) {
        try {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor next = writer;

            next = new LowerBytecodeVersion(next, targetVersion);
            if (targetVersion < Opcodes.V1_7) {
                next = new SwallowSuppressedExceptions(next);
                next = new RemoveMethodHandlesLookupReferences(next);
                next = new RequireNonNull(next);
            }
            next = new FixInvokeStaticOnInterfaceMethod(next);
            next = new UpdateRenamedEnclosingMethods(next, analyzer);
            next = chain.wrap(next);

            reader.accept(next);
            return writer.toByteArray();

        } catch (Throwable t) {
            throw new RuntimeException("Failed to backport class: " + className, t);
        }
    }

    private interface ClassVisitorChain {
        ClassVisitor wrap(ClassVisitor next);
    }
}
