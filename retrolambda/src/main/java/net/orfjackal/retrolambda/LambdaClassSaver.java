// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.nio.file.*;

public class LambdaClassSaver {

    private final Path outputDir;
    private final int bytecodeVersion;

    public LambdaClassSaver(Path outputDir, int bytecodeVersion) {
        this.outputDir = outputDir;
        this.bytecodeVersion = bytecodeVersion;
    }

    public void saveIfLambda(String className, byte[] bytecode) {
        if (LambdaReifier.isLambdaClassToReify(className)) {
            reifyLambdaClass(className, bytecode);
        }
    }

    private void reifyLambdaClass(String className, byte[] originalBytecode) {
        try {
            System.out.println("Saving lambda class: " + className);
            byte[] backportedBytecode = LambdaClassBackporter.transform(originalBytecode, bytecodeVersion);
            Path savePath = outputDir.resolve(className + ".class");
            Files.createDirectories(savePath.getParent());
            Files.write(savePath, backportedBytecode);

        } catch (Throwable t) {
            // print to stdout to keep in sync with other log output
            System.out.println("ERROR: Failed to backport lambda class: " + className);
            t.printStackTrace(System.out);
        }
    }
}
