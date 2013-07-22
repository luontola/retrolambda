// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.IOException;
import java.lang.instrument.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class LambdaSavingClassFileTransformer implements ClassFileTransformer {

    private static final Pattern LAMBDA_CLASS = Pattern.compile(".+\\$\\$Lambda\\$\\d+$");

    private static final BlockingDeque<String> foundLambdaClasses = new LinkedBlockingDeque<>(1); // we expect only one at a time
    private final Path outputDir;

    public LambdaSavingClassFileTransformer(Path outputDir) {
        this.outputDir = outputDir;
    }

    public static String getLastFoundLambdaClass() {
        return foundLambdaClasses.pop();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!LAMBDA_CLASS.matcher(className).matches()) {
            return null;
        }
        try {
            System.out.println("Saving lambda class: " + className);
            foundLambdaClasses.push(className);
            byte[] transformedBytes = LambdaClassBackporter.transform(classfileBuffer);
            Path savePath = outputDir.resolve(className + ".class");
            Files.createDirectories(savePath.getParent());
            Files.write(savePath, transformedBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
