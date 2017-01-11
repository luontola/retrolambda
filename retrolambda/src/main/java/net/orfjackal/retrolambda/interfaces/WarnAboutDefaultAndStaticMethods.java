// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.esotericsoftware.minlog.Log;
import org.objectweb.asm.*;

import static net.orfjackal.retrolambda.util.Flags.*;

public class WarnAboutDefaultAndStaticMethods extends ClassVisitor {

    private String interfaceName;

    public WarnAboutDefaultAndStaticMethods(ClassVisitor next) {
        super(Opcodes.ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (!isInterface(access)) {
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
        // - static initialization blocks (for initializing constants)
        if (isStaticMethod(access)) {
            if (!isStaticInitializer(name, desc, access) &&
                    !name.startsWith("lambda$")) {
                printWarning("a static method", name);
            }
        } else {
            if (!isAbstractMethod(access)) {
                printWarning("a default method", name);
            }
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private void printWarning(String methodKind, String methodName) {
        Log.warn("The interface " + interfaceName + " has " + methodKind + " \"" + methodName + "\" " +
                "but backporting default methods is not enabled");
    }
}
