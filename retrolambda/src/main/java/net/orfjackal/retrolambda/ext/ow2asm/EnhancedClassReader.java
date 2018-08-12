// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.ext.ow2asm;

import org.objectweb.asm.*;

public class EnhancedClassReader extends ClassReader {

    private final boolean isJavacHacksEnabled;

    public EnhancedClassReader(byte[] b, boolean isJavacHacksEnabled) {
        super(b);
        this.isJavacHacksEnabled = isJavacHacksEnabled;
    }

    @Override
    protected Label readLabel(int offset, Label[] labels) {
        if (!isJavacHacksEnabled) {
            return super.readLabel(offset, labels);
        }
        // A workaround suggested by Evgeny Mandrikov. See more: https://gitlab.ow2.org/asm/asm/issues/317845
        return super.readLabel(Math.min(offset, labels.length - 1), labels);
    }

}
