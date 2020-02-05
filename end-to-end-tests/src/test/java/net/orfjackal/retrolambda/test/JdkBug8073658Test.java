// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.lang.annotation.*;

public class JdkBug8073658Test {

    // Some versions of `javac` produce incorrect bytecode which
    // causes a crash in ASM. See the following issues for details.
    // https://github.com/luontola/retrolambda/pull/143
    // https://gitlab.ow2.org/asm/asm/issues/317845
    // https://bugs.openjdk.java.net/browse/JDK-8073658
    @Test
    public void test() {
        new ClassWithBridgeMethod<>().doit(null);
    }
}

class ClassWithBridgeMethod<T extends Runnable> implements ParameterizedInterface<T> {

    @Override
    public void doit(final T t) {

        @TypeUseAnnotation
        Object x = null;

        // Some instructions just to widen the scope of annotation for x
        System.out.println(x);
        System.out.println(x);
    }

}

interface ParameterizedInterface<T> {
    void doit(T arg);
}

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.CLASS)
@interface TypeUseAnnotation {
}
