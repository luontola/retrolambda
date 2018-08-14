// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import net.orfjackal.retrolambda.ext.ow2asm.EnhancedClassReader;

import java.lang.instrument.*;
import java.security.ProtectionDomain;

public class LambdaClassSaverAgent implements ClassFileTransformer {

    private LambdaClassSaver lambdaClassSaver;
    private boolean isJavacHacksEnabled;

    public void setLambdaClassSaver(LambdaClassSaver lambdaClassSaver, boolean isJavacHacksEnabled) {
        this.lambdaClassSaver = lambdaClassSaver;
        this.isJavacHacksEnabled = isJavacHacksEnabled;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) {
            // Since JDK 8 build b121 or so, lambda classes have a null class name,
            // but we can read it from the bytecode where the name still exists.
            className = EnhancedClassReader.create(classfileBuffer, isJavacHacksEnabled).getClassName();
        }
        if (lambdaClassSaver != null) {
            lambdaClassSaver.saveIfLambda(className, classfileBuffer);
        }
        return null;
    }
}
