// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.lang.invoke.*;
import java.lang.reflect.*;

public class Spike {

    public static void main(String[] args) throws Exception {
        Class<?> targetClass = Dummy.class;
        ClassLoader targetClassClassLoader = targetClass.getClassLoader();

        Constructor<MethodHandles.Lookup> ctor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
        ctor.setAccessible(true);
        MethodHandles.Lookup caller = ctor.newInstance(targetClass);

        LambdaMetafactory.metafactory(
                caller,
                "run",
                MethodType.fromMethodDescriptorString("()Ljava/lang/Runnable;", targetClassClassLoader),
                MethodType.fromMethodDescriptorString("()V", targetClassClassLoader),
                caller.findStatic(targetClass, "lambda$0", MethodType.fromMethodDescriptorString("()V", targetClassClassLoader)),
                MethodType.fromMethodDescriptorString("()V", targetClassClassLoader));
    }

    private static MethodHandles.Lookup getTrustedLookup() throws Exception {
        Field f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        f.setAccessible(true);
        return (MethodHandles.Lookup) f.get(null);
    }
}

class Dummy {

    public void foo() {
        Runnable r = () -> {
            System.out.println("foo");
        };
    }
}