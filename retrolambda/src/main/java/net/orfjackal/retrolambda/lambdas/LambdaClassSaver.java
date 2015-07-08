// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import net.orfjackal.retrolambda.Transformers;
import net.orfjackal.retrolambda.files.ClassSaver;
import org.objectweb.asm.ClassReader;

public class LambdaClassSaver {

    private final ClassSaver saver;
    private final Transformers transformers;

    public LambdaClassSaver(ClassSaver saver, Transformers transformers) {
        this.saver = saver;
        this.transformers = transformers;
    }

    public void saveIfLambda(String className, byte[] bytecode) {
        if (LambdaReifier.isLambdaClassToReify(className)) {
            reifyLambdaClass(className, bytecode);
        }
    }

    private void reifyLambdaClass(String className, byte[] bytecode) {
        try {
            System.out.println("Saving lambda class: " + className);
            saver.saveClass(transformers.backportLambdaClass(new ClassReader(bytecode)));

        } catch (Throwable t) {
            // print to stdout to keep in sync with other log output
            System.out.println("ERROR: Failed to backport lambda class: " + className);
            t.printStackTrace(System.out);
        }
    }
}
