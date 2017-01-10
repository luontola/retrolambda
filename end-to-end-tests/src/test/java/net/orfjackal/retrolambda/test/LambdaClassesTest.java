// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static net.orfjackal.retrolambda.test.TestUtil.assertClassExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LambdaClassesTest {

    @Test
    public void the_sequence_number_starts_from_1_for_each_enclosing_class() {
        assertClassExists(Dummy1.class.getName() + "$$Lambda$1");
        assertClassExists(Dummy1.class.getName() + "$$Lambda$2");
        assertClassExists(Dummy2.class.getName() + "$$Lambda$1");
        assertClassExists(Dummy2.class.getName() + "$$Lambda$2");
    }

    @SuppressWarnings("UnusedDeclaration")
    private class Dummy1 {
        private Dummy1() {
            Runnable lambda1 = () -> {
            };
            Runnable lambda2 = () -> {
            };
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private class Dummy2 {
        private Dummy2() {
            Runnable lambda1 = () -> {
            };
            Runnable lambda2 = () -> {
            };
        }
    }


    @Test
    public void capturing_lambda_classes_contain_no_unnecessary_methods() throws ClassNotFoundException {
        assertThat(getMethodNames(findLambdaClass(Capturing.class)),
                is(ImmutableSet.of("lambdaFactory$", "run")));
    }

    @SuppressWarnings("UnusedDeclaration")
    private class Capturing {
        private Capturing() {
            Runnable lambda = () -> System.out.println(hashCode());
        }
    }


    @Test
    public void non_capturing_lambda_classes_contain_no_unnecessary_methods() throws ClassNotFoundException {
        assertThat(getMethodNames(findLambdaClass(NonCapturing.class)),
                is(ImmutableSet.of("lambdaFactory$", "run")));
    }

    @SuppressWarnings("UnusedDeclaration")
    private class NonCapturing {
        private NonCapturing() {
            Runnable lambda = () -> {
            };
        }
    }

    @Test
    public void enclosing_classes_contain_no_unnecessary_methods_in_addition_to_the_lambda_body() throws ClassNotFoundException {
        assertThat("non-capturing lambda", getMethodNames(NonCapturing.class), contains(startsWith("lambda$new$")));
        assertThat("capturing lambda", getMethodNames(Capturing.class), contains(startsWith("lambda$new$")));
    }

    @Test
    public void does_not_contain_references_to_JDK_lambda_classes() throws IOException {
        ClassReader cr = new ClassReader("net/orfjackal/retrolambda/test/LambdaClassesTest$Dummy1$$Lambda$1");

        // XXX: fix visitConstantPool and assert the constant pool entries instead of this hack
//        TestUtil.visitConstantPool(cr, (item, constant) -> {
//        });

        String bytecode = new String(cr.b);
        assertThat(bytecode, not(containsString("java/lang/invoke/LambdaForm")));
    }


    // helpers

    private static Class<?> findLambdaClass(Class<?> clazz) throws ClassNotFoundException {
        return Class.forName(clazz.getName() + "$$Lambda$1");
    }

    private static Set<String> getMethodNames(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        Set<String> uniqueNames = new HashSet<>();
        for (Method method : methods) {
            uniqueNames.add(method.getName());
        }
        assertThat("unexpected overloaded methods", methods, arrayWithSize(uniqueNames.size()));
        return uniqueNames;
    }
}
