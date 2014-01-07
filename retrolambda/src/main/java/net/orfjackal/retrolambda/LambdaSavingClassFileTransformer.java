// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.lang.instrument.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;

public class LambdaSavingClassFileTransformer implements ClassFileTransformer {

    private final Path outputDir;
    private final int targetVersion;
    private final List<ClassLoader> ignoredClassLoaders = new ArrayList<>();

    public LambdaSavingClassFileTransformer(Path outputDir, int targetVersion) {
        this.outputDir = outputDir;
        this.targetVersion = targetVersion;
        for (ClassLoader cl = ClassLoader.getSystemClassLoader(); cl != null; cl = cl.getParent()) {
            ignoredClassLoaders.add(cl);
        }
        ignoredClassLoaders.add(null);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (ignoredClassLoaders.contains(loader)) {
            // Avoid saving any classes from the JDK or Retrolambda itself.
            // The transformed application classes have their own class loader.
            return null;
        }
        if (className == null) {
            // Since JDK 8 build b121 or so, lambda classes have a null class name,
            // but we can read it from the bytecode where the name still exists.
            className = new ClassReader(classfileBuffer).getClassName();
        }
        if (!isLambdaClass(className)) {
            return null;
        }
        try {
            System.out.println("Saving lambda class: " + className);
            byte[] backportedBytecode = LambdaClassBackporter.transform(classfileBuffer, targetVersion);
            Path savePath = outputDir.resolve(className + ".class");
            Files.createDirectories(savePath.getParent());
            Files.write(savePath, backportedBytecode);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isLambdaClass(String className) {
        return LambdaNaming.LAMBDA_CLASS.matcher(className).matches();
    }
}
