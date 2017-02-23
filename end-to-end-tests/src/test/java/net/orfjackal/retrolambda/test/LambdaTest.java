// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import net.orfjackal.retrolambda.test.anotherpackage.DifferentPackageBase;
import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

public class LambdaTest extends SuperClass {

    @Test
    public void empty_lambda() {
        Runnable lambda = () -> {
        };

        lambda.run();
    }

    @Test
    public void lambda_returning_a_value() throws Exception {
        Callable<String> lambda = () -> "some value";

        assertThat(lambda.call(), is("some value"));
    }

    private interface Function1<IN, OUT> {
        OUT apply(IN value);
    }

    @Test
    public void lambda_taking_parameters() {
        Function1<String, Integer> lambda = (String s) -> s.getBytes().length;

        assertThat(lambda.apply("foo"), is(3));
    }

    @Test
    public void lambda_in_project_main_sources() throws Exception {
        assertThat(InMainSources.callLambda(), is(42));
    }

    private int instanceVar = 0;

    @Test
    public void lambda_using_instance_variables() {
        Runnable lambda = () -> {
            instanceVar = 42;
        };
        lambda.run();

        assertThat(instanceVar, is(42));
    }

    @Test
    public void lambda_using_local_variables() {
        int[] localVar = new int[1];
        Runnable lambda = () -> {
            localVar[0] = 42;
        };
        lambda.run();

        assertThat(localVar[0], is(42));
    }

    @Test
    public void lambda_using_local_variables_of_primitive_types() throws Exception {
        boolean bool = true;
        byte b = 2;
        short s = 3;
        int i = 4;
        long l = 5;
        float f = 6;
        double d = 7;
        char c = 8;
        Callable<Integer> lambda = () -> (int) ((bool ? 1 : 0) + b + s + i + l + f + d + c);

        assertThat(lambda.call(), is(36));
    }

    @Test
    public void lambda_in_the_constant_initializer_of_an_interface() throws Exception {
        assertThat(LambdaConstant.LAMBDA.call(), is("foo"));
    }

    public interface LambdaConstant {
        Callable<String> LAMBDA = () -> "foo";
    }

    @Test
    public void lambdas_with_marker_interfaces_due_to_intersection_types() throws Exception {
        // We must use something other than java.io.Serializable as the marker interface,
        // because serializable lambdas are signified by a flag to LambdaMetafactory.altMetafactory
        Callable<String> lambda = (Callable<String> & Cloneable) () -> "foo";

        assertThat(lambda, is(instanceOf(Cloneable.class)));
        assertThat(lambda.call(), is("foo"));
    }

    @Test
    public void method_references_to_virtual_methods_on_local_variables() throws Exception {
        String foo = "foo";
        Callable<String> ref = foo::toUpperCase;

        assertThat(ref.call(), is("FOO"));
    }

    @Test
    public void method_references_to_virtual_methods_on_instance_variables() throws Exception {
        Callable<String> ref = instanceVarFoo::toUpperCase;

        assertThat(ref.call(), is("FOO"));
    }

    @SuppressWarnings("FieldCanBeLocal")
    private String instanceVarFoo = "foo";

    @Test
    public void method_references_to_interface_methods() throws Exception {
        List<String> foos = Arrays.asList("foo");
        Callable<Integer> ref = foos::size;

        assertThat(ref.call(), is(1));
    }

    @Test
    public void method_references_to_static_methods() throws Exception {
        long expected = System.currentTimeMillis();
        Callable<Long> ref = System::currentTimeMillis;

        assertThat(ref.call(), is(greaterThanOrEqualTo(expected)));
    }

    @Test
    public void method_references_to_constructors() throws Exception {
        Callable<List<String>> ref = ArrayList<String>::new;

        assertThat(ref.call(), is(instanceOf(ArrayList.class)));
    }

    @Test
    public void method_references_to_protected_supertype_methods() throws Exception {
        Callable<String> ref1 = new SubclassInMyPackage().thing();
        assertThat(ref1.call(), equalTo("Hello"));

        Callable<String> ref2 = new SubclassInSamePackage().thing();
        assertThat(ref2.call(), equalTo("Hello"));
    }

    public static class SubclassInMyPackage extends DifferentPackageBase {
        public Callable<String> thing() {
            return DifferentPackageBase::value;
        }
    }

    public static class SubclassInSamePackage extends SamePackageBase {
        public Callable<String> thing() {
            return SamePackageBase::value;
        }
    }

    /**
     * Because the constructor is private, an access method must be generated for it
     * and also the NEW instruction must be done inside the access method.
     */
    @Test
    public void method_references_to_private_constructors() throws Exception {
        Callable<HasPrivateConstructor> factory = HasPrivateConstructor.factory();
        assertThat(factory.call(), is(instanceOf(HasPrivateConstructor.class)));

        HasPrivateConstructorWithArgs.Factory factoryArgs = HasPrivateConstructorWithArgs.factory();
        assertThat(factoryArgs.create("args"), is(instanceOf(HasPrivateConstructorWithArgs.class)));
        assertThat(factoryArgs.create("args").args, is("args"));
    }

    public static class HasPrivateConstructor {

        private HasPrivateConstructor() {
        }

        public static Callable<HasPrivateConstructor> factory() {
            return HasPrivateConstructor::new;
        }
    }

    public static class HasPrivateConstructorWithArgs {
        public final String args;

        private HasPrivateConstructorWithArgs(String args) {
            this.args = args;
        }

        public static Factory factory() {
            return HasPrivateConstructorWithArgs::new;
        }

        public interface Factory {
            HasPrivateConstructorWithArgs create(String args);
        }
    }


    @Test
    public void method_references_to_overridden_inherited_methods_with_super() throws Exception {
        Callable<String> ref = super::inheritedMethod;

        assertThat(ref.call(), is("superclass version"));
    }

    @Override
    String inheritedMethod() {
        return "overridden version";
    }

    @Test
    public void method_references_to_private_methods() throws Exception {
        Callable<String> ref1 = LambdaTest::privateClassMethod;
        assertThat(ref1.call(), is("foo"));

        Callable<String> ref2 = this::privateInstanceMethod;
        assertThat(ref2.call(), is("foo"));

        // Normal method calls should still work after our magic
        // of making them them accessible from the lambda classes.
        assertThat(privateClassMethod(), is("foo"));
        assertThat(privateInstanceMethod(), is("foo"));
    }

    private String privateInstanceMethod() {
        return "foo";
    }

    private static String privateClassMethod() {
        return "foo";
    }


    @Test
    public void enclosing_method_of_anonymous_class_inside_lambda_expression() throws Exception {
        Callable<Object> lambda = () -> new Object() {
        };
        Class<?> anonymousClass = lambda.call().getClass();

        assertThat(anonymousClass.getEnclosingMethod().getName(),
                startsWith("lambda$enclosing_method_of_anonymous_class_inside_lambda_expression$"));
    }

    /**
     * We could make private lambda implementation methods package-private,
     * so that the lambda class may call them, but we should not make any
     * more methods non-private than is absolutely necessary.
     */
    @Test
    public void will_not_change_the_visibility_of_unrelated_methods() throws Exception {
        assertThat(unrelatedPrivateMethod(), is("foo"));

        Method method = getClass().getDeclaredMethod("unrelatedPrivateMethod");
        int modifiers = method.getModifiers();

        assertTrue("expected " + method.getName() + " to be private, but modifiers were: " + Modifier.toString(modifiers),
                Modifier.isPrivate(modifiers));
    }

    private String unrelatedPrivateMethod() {
        return "foo";
    }

    /**
     * We cannot just make the private methods package-private for the
     * lambda class to call them, because that may cause a method in subclass
     * to override them.
     */
    @Test
    public void will_not_cause_private_methods_to_be_overridable() throws Exception {
        class Parent {
            private String privateMethod() {
                return "parent version";
            }

            Callable<String> parentRef() {
                return this::privateMethod;
            }
        }
        class Child extends Parent {
            private String privateMethod() { // would override if were not private
                return "child version";
            }

            Callable<String> childRef() {
                return this::privateMethod;
            }
        }

        Child child = new Child();

        // Our test assumes that there exists a private method with
        // the same name and signature in super and sub classes.
        String name = "privateMethod";
        assertThat(child.getClass().getDeclaredMethod(name), is(notNullValue()));
        assertThat(child.getClass().getSuperclass().getDeclaredMethod(name), is(notNullValue()));

        assertThat(child.privateMethod(), is("child version"));
        assertThat(child.childRef().call(), is("child version"));

        assertThat(((Parent) child).privateMethod(), is("parent version"));
        assertThat(child.parentRef().call(), is("parent version"));
    }

    /**
     * If the lambda impl method is generated as a private instance method,
     * we cannot just make it package-private for the lambda class to call them,
     * because a subclass may override the lambda by overriding its enclosing method
     * and declaring another lambda expression there.
     */
    @Test
    public void will_not_cause_lambda_expressions_to_be_overridable() {
        List<String> spy = new ArrayList<>();
        class Parent {
            @SuppressWarnings("unused")
            private int i;

            public void foo() {
                Runnable lambda = () -> { // generates a private "lambda$foo$0" method
                    i++; // causes this lambda to be generated as an instance method
                    spy.add("parent");
                };
                lambda.run();
            }
        }
        class Child extends Parent {
            @SuppressWarnings("unused")
            private int i;

            @Override
            public void foo() {
                super.foo();
                Runnable lambda = () -> { // generates a private "lambda$foo$0" method
                    i++; // causes this lambda to be generated as an instance method
                    spy.add("child");
                };
                lambda.run();
            }
        }

        Child c = new Child();
        c.foo();

        assertThat(spy, is(Arrays.asList("parent", "child")));
    }

    @Test
    public void bytecode_constant_pool_will_not_contain_dangling_references_to_MethodHandles() throws IOException {
        assumeThat(SystemUtils.JAVA_VERSION_FLOAT, is(lessThan(1.7f)));

        ClassReader cr = new ClassReader(getClass().getName().replace('.', '/'));
        TestUtil.visitConstantPool(cr, (item, constant) -> {
            if (constant instanceof Type) {
                Type type = (Type) constant;
                assertThat("constant #" + item, type.getDescriptor(), not(containsString("java/lang/invoke")));
            }
        });
    }
}

class SuperClass {
    String inheritedMethod() {
        return "superclass version";
    }
}
