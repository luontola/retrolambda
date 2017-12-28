// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import com.google.common.collect.ImmutableSet;
import org.apache.bcel.classfile.*;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

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
        ConstantPool constantPool = TestUtil.getConstantPool("net/orfjackal/retrolambda/test/LambdaClassesTest$Dummy1$$Lambda$1");

        for (Constant constant : constantPool.getConstantPool()) {
            if (constant != null) {
                String s = constantPool.constantToString(constant);
                assertThat(s, not(containsString("java/lang/invoke/LambdaForm")));
            }
        }
    }

    @Test
    public void has_the_same_source_file_attribute_as_the_enclosing_class() throws IOException {
        ClassNode enclosing = readClass("net/orfjackal/retrolambda/test/LambdaClassesTest");
        ClassNode lambda = readClass("net/orfjackal/retrolambda/test/LambdaClassesTest$Dummy1$$Lambda$1");

        assertThat(lambda.sourceFile, is(notNullValue()));
        assertThat(lambda.sourceFile, is(enclosing.sourceFile));
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

    private static ClassNode readClass(String name) throws IOException {
        ClassReader cr = new ClassReader(name);
        ClassNode cls = new ClassNode();
        cr.accept(cls, ClassReader.SKIP_CODE);
        return cls;
    }
}
