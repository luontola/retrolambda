// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import com.esotericsoftware.minlog.Log;
import net.orfjackal.retrolambda.Transformers;
import net.orfjackal.retrolambda.ext.ow2asm.EnhancedClassReader;
import net.orfjackal.retrolambda.files.OutputDirectory;

import java.io.IOException;

public class LambdaClassSaver {

    private final OutputDirectory saver;
    private final Transformers transformers;
    private final boolean isJavacHacksEnabled;

    public LambdaClassSaver(OutputDirectory saver, Transformers transformers, boolean isJavacHacksEnabled) {
        this.saver = saver;
        this.transformers = transformers;
        this.isJavacHacksEnabled = isJavacHacksEnabled;
    }

    public void saveIfLambda(String className, byte[] bytecode) {
        if (LambdaReifier.isLambdaClassToReify(className)) {
            reifyLambdaClass(className, bytecode);
        }
    }

    private void reifyLambdaClass(String className, byte[] bytecode) {
        Log.info("Saving lambda class: " + className);
        bytecode = transformers.backportLambdaClass(EnhancedClassReader.create(bytecode, isJavacHacksEnabled));
        try {
            saver.writeClass(bytecode, isJavacHacksEnabled);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
