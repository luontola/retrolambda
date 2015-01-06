// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.util;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class Flags {

    public static boolean hasFlag(int subject, int flag) {
        return (subject & flag) == flag;
    }

    public static boolean isClassInitializer(String name, String desc, int methodAccess) {
        return name.equals("<clinit>") &&
                desc.equals("()V") &&
                hasFlag(methodAccess, ACC_STATIC);
    }
}
