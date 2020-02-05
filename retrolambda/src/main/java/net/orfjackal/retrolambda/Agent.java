// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.ext.ow2asm.EnhancedClassReader;
import net.orfjackal.retrolambda.lambdas.LambdaClassSaver;
import org.objectweb.asm.ClassReader;

public class Agent {

    private static boolean enabled = false;
    private static LambdaClassSaver lambdaClassSaver;
    private static boolean isJavacHacksEnabled;

    public static void enable() {
        enabled = true;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setLambdaClassSaver(LambdaClassSaver lambdaClassSaver, boolean isJavacHacksEnabled) {
        Agent.lambdaClassSaver = lambdaClassSaver;
        Agent.isJavacHacksEnabled = isJavacHacksEnabled;
    }

    public static void saveLambda(byte[] bytes) {
        if (lambdaClassSaver != null) {
            ClassReader reader = EnhancedClassReader.create(bytes, isJavacHacksEnabled);
            lambdaClassSaver.saveIfLambda(reader.getClassName(), bytes);
        }
    }
}
