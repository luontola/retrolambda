// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import net.orfjackal.retrolambda.util.Flags;

import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

public class LambdaNaming {

    public static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";
    public static final String MAGIC_LAMBDA_IMPL = "java/lang/invoke/MagicLambdaImpl";

    /**
     * Java 8 produces at runtime classes named {@code EnclosingClass$$Lambda$1}
     */
    public static final Pattern LAMBDA_CLASS = Pattern.compile("^.+\\$\\$Lambda\\$\\d+$");

    public static boolean isSerializationHook(int access, String name, String desc) {
        return name.equals("writeReplace")
                && desc.equals("()Ljava/lang/Object;")
                && Flags.hasFlag(access, ACC_PRIVATE);
    }

    public static boolean isDeserializationHook(int access, String name, String desc) {
        return name.equals("$deserializeLambda$")
                && desc.equals("(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;")
                && Flags.hasFlag(access, ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC);
    }

    public static boolean isPlatformFactoryMethod(int access, String name, String desc, String targetDesc) {
        return name.equals("get$Lambda")
                && desc.equals(targetDesc)
                && Flags.hasFlag(access, ACC_PRIVATE | ACC_STATIC);
    }

    public static boolean isBodyMethodName(String name) {
        return name.startsWith("lambda$");
    }

    public static boolean isBodyMethod(int access, String name) {
        return isBodyMethodName(name) && Flags.hasFlag(access, ACC_SYNTHETIC);
    }
}
