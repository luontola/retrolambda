// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.IOException;
import java.lang.instrument.*;
import java.nio.file.*;
import java.security.ProtectionDomain;

public class SpikeAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new MyClassFileTransformer());
    }
}

class MyClassFileTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!className.contains("$$Lambda$")) {
            return null;
        }
        try {
            System.out.println(className);
            Path savePath = Paths.get("generated", className + ".class");
            Files.createDirectories(savePath.getParent());
            Files.write(savePath, classfileBuffer);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
