// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.Handle;

import static org.objectweb.asm.Opcodes.*;

public class Handles {

    public static int getOpcode(Handle handle) {
        int tag = handle.getTag();
        switch (tag) {
            case H_INVOKEVIRTUAL:
                return INVOKEVIRTUAL;
            case H_INVOKESTATIC:
                return INVOKESTATIC;
            case H_INVOKESPECIAL:
                return INVOKESPECIAL;
            case H_NEWINVOKESPECIAL:
                return INVOKESPECIAL; // we assume that the caller takes care of the NEW instruction
            case H_INVOKEINTERFACE:
                return INVOKEINTERFACE;
            default:
                throw new IllegalArgumentException("Unsupported tag " + tag + " in " + handle);
        }
    }
}
