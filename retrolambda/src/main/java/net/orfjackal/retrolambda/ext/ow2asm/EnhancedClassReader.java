// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.ext.ow2asm;

import org.objectweb.asm.*;

public class EnhancedClassReader extends ClassReader {

    public static ClassReader create(byte[] bytecode, boolean isJavacHacksEnabled) {
        if (isJavacHacksEnabled) {
            return new EnhancedClassReader(bytecode);
        } else {
            return new ClassReader(bytecode);
        }
    }

    private EnhancedClassReader(byte[] b) {
        super(b);
    }

    @Override
    protected Label readLabel(int offset, Label[] labels) {
        // A workaround suggested by Evgeny Mandrikov. See more: https://gitlab.ow2.org/asm/asm/issues/317845
        return super.readLabel(Math.min(offset, labels.length - 1), labels);
    }
}
