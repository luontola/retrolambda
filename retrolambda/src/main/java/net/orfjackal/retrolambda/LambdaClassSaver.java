// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

public class LambdaClassSaver {

    private final ClassSaver saver;
    private final int bytecodeVersion;
    private MethodRelocations methodRelocations;

    public LambdaClassSaver(ClassSaver saver, int bytecodeVersion, MethodRelocations methodRelocations) {
        this.saver = saver;
        this.bytecodeVersion = bytecodeVersion;
        this.methodRelocations = methodRelocations;
    }

    public void setMethodRelocations(MethodRelocations methodRelocations) {
        this.methodRelocations = methodRelocations;
    }

    public void saveIfLambda(String className, byte[] bytecode) {
        if (LambdaReifier.isLambdaClassToReify(className)) {
            reifyLambdaClass(className, bytecode);
        }
    }

    private void reifyLambdaClass(String className, byte[] bytecode) {
        try {
            System.out.println("Saving lambda class: " + className);
            saver.save(LambdaClassBackporter.transform(bytecode, bytecodeVersion, methodRelocations));

        } catch (Throwable t) {
            // print to stdout to keep in sync with other log output
            System.out.println("ERROR: Failed to backport lambda class: " + className);
            t.printStackTrace(System.out);
        }
    }
}
