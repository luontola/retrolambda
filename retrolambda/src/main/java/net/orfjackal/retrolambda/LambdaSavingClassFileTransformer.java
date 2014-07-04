// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.ClassReader;

import java.lang.instrument.*;
import java.nio.file.*;
import java.security.ProtectionDomain;

public class LambdaSavingClassFileTransformer implements ClassFileTransformer {

    private final Path outputDir;
    private final int targetVersion;

    public LambdaSavingClassFileTransformer(Path outputDir, int targetVersion) {
        this.outputDir = outputDir;
        this.targetVersion = targetVersion;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) {
            // Since JDK 8 build b121 or so, lambda classes have a null class name,
            // but we can read it from the bytecode where the name still exists.
            className = new ClassReader(classfileBuffer).getClassName();
        }
        if (LambdaReifier.isLambdaClassToReify(className)) {
            reifyLambdaClass(className, classfileBuffer);
        }
        return null;
    }

    private void reifyLambdaClass(String className, byte[] classfileBuffer) {
        try {
            System.out.println("Saving lambda class: " + className);
            byte[] backportedBytecode = LambdaClassBackporter.transform(classfileBuffer, targetVersion);
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
