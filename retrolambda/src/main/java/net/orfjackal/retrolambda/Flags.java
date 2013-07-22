// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.Opcodes;

public class Flags {

    public static int makeNonPrivate(int access) {
        if (hasFlag(access, Opcodes.ACC_PRIVATE)) {
            return clearFlag(access, Opcodes.ACC_PRIVATE); // make package-private (i.e. no flag)
        }
        return access;
    }

    public static boolean hasFlag(int subject, int flag) {
        return (subject & flag) == flag;
    }

    public static int clearFlag(int subject, int flag) {
        return subject & ~flag;
    }
}
