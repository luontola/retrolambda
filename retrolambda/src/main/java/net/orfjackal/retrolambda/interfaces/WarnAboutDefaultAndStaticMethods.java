// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.Flags;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class WarnAboutDefaultAndStaticMethods extends ClassVisitor {

    private String interfaceName;

    public WarnAboutDefaultAndStaticMethods(ClassVisitor next) {
        super(Opcodes.ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (!Flags.hasFlag(access, ACC_INTERFACE)) {
            throw new IllegalArgumentException(name + " is not an interface");
        }
        interfaceName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // Not allowed by Java 7 in interfaces:
        // - default methods
        // - static methods
        // - bridge methods
        // Allowed by Java 7:
        // - class initializer methods (for initializing constants)
        if (Flags.hasFlag(access, ACC_STATIC)) {
            if (!Flags.isClassInitializer(name, desc, access) &&
                    !name.startsWith("lambda$")) {
                printWarning("a static method", name);
            }
        } else {
            if (!Flags.hasFlag(access, ACC_ABSTRACT)) {
                printWarning("a default method", name);
            }
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private void printWarning(String methodKind, String methodName) {
        System.out.println("WARNING: The interface " + interfaceName + " has " + methodKind + " \"" + methodName + "\" " +
                "but backporting default methods is not enabled");
    }
}
