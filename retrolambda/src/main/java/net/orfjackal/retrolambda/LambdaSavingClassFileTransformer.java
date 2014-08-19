// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.ClassReader;

import java.lang.instrument.*;
import java.security.ProtectionDomain;

public class LambdaSavingClassFileTransformer implements ClassFileTransformer {

    private final LambdaClassSaver lambdaClassSaver;

    public LambdaSavingClassFileTransformer(LambdaClassSaver lambdaClassSaver) {
        this.lambdaClassSaver = lambdaClassSaver;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) {
            // Since JDK 8 build b121 or so, lambda classes have a null class name,
            // but we can read it from the bytecode where the name still exists.
            className = new ClassReader(classfileBuffer).getClassName();
        }
        lambdaClassSaver.saveIfLambda(className, classfileBuffer);
        return null;
    }
}
