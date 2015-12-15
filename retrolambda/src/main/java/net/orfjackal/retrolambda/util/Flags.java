// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.util;

import static org.objectweb.asm.Opcodes.*;

public class Flags {

    public static boolean hasFlag(int subject, int flag) {
        return (subject & flag) == flag;
    }

    // classes

    public static boolean isInterface(int access) {
        return hasFlag(access, ACC_INTERFACE);
    }

    // initialization

    public static boolean isConstructor(String name) {
        return name.equals("<init>");
    }

    public static boolean isStaticInitializer(String name, String desc, int access) {
        return name.equals("<clinit>") &&
                desc.equals("()V") &&
                hasFlag(access, ACC_STATIC);
    }

    // concrete vs abstract

    public static boolean isConcreteMethod(int access) {
        return !isAbstractMethod(access);
    }

    public static boolean isAbstractMethod(int access) {
        return hasFlag(access, ACC_ABSTRACT);
    }

    // instance vs static

    public static boolean isInstanceMethod(int access) {
        return !isStaticMethod(access);
    }

    public static boolean isStaticMethod(int access) {
        return hasFlag(access, ACC_STATIC);
    }

    // visibility

    public static boolean isPublicMethod(int access) {
        return hasFlag(access, ACC_PUBLIC);
    }

    public static boolean isPrivateMethod(int access) {
        return hasFlag(access, ACC_PRIVATE);
    }
}
