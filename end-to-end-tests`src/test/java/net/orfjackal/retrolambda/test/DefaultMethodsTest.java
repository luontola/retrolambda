// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import net.orfjackal.retrolambda.test.anotherpackage.UsesLambdasInAnotherPackage;
import org.apache.commons.lang.SystemUtils;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.Callable;

import static net.orfjackal.retrolambda.test.TestUtil.companionOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef", "RedundantCast", "UnusedDeclaration"})
public class DefaultMethodsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();


    // Inheriting & Overriding

    @Test
    public void default_method_inherited_from_interface() {
        DefaultMethods obj = new DefaultMethods() {
        };
        assertThat(obj.foo(), is("original"));
    }

    @Test
    public void default_method_overridden_in_current_class() {
        assertThat(new DefaultMethodOverridingClass().foo(), is("overridden"));
    }

    @Test
    public void default_method_overridden_in_parent_class() {
        class C extends DefaultMethodOverridingClass {
        }
        assertThat(new C().foo(), is("overridden"));
    }

    @Test
    public void default_method_overridden_in_parent_class_and_implements_interface_explicitly() {
        class C extends DefaultMethodOverridingClass implements DefaultMethods {
        }
        assertThat(new C().foo(), is("overridden"));
    }

    private interface DefaultMethods {
        default String foo() {
            return "original";
        }
    }

    private class DefaultMethodOverridingClass implements DefaultMethods {
        @Override
        public String foo() {
            return "overridden";
        }
    }


    @Test
    public void default_method_overridden_in_child_interface() {
        OverrideChild child = new OverrideChild() {
        };
        assertThat(child.foo(), is("overridden"));
    }

    private interface OverrideParent {
        default String foo() {
            return "original";
        }
    }

    private interface OverrideChild extends OverrideParent {
        @Override
        default String foo() {
            return "overridden";
        }
    }


    /**
     * Based on the example in <a href="http://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.4.1">JLS §9.4.1</a>
     * (Interfaces - Inheritance and Overriding)
     */
    @Test
    public void inheriting_same_default_methods_through_many_parent_interfaces() {
        assertThat(new InheritsOriginal() {
        }.foo(), is("original"));

        assertThat(new InheritsOverridden() {
        }.foo(), is("overridden"));

        assertThat(new InheritsOverriddenAndOriginal() {
        }.foo(), is("overridden"));

        assertThat(new InheritsOriginalAndOverridden() {
        }.foo(), is("overridden"));
    }

    private interface SuperOriginal {
        default String foo() {
            return "original";
        }
    }

    private interface SuperOverridden extends SuperOriginal {
        @Override
        default String foo() {
            return "overridden";
        }
    }

    private interface InheritsOriginal extends SuperOriginal {
    }

    private interface InheritsOverridden extends SuperOverridden {
    }

    private interface InheritsOverriddenAndOriginal extends SuperOverridden, InheritsOriginal {
    }

    private interface InheritsOriginalAndOverridden extends InheritsOriginal, SuperOverridden {
    }


    @Test
    public void implements_original_and_overridden_default_method() {
        assertThat(new ImplementsOriginal().foo(), is("original"));
        assertThat(new ImplementsOriginalAndOverriddenDefault().foo(), is("overridden"));
        assertThat(new ImplementsOverriddenAndOriginalDefault().foo(), is("overridden"));
        assertThat(new ExtendsImplementsOriginalAndImplementsOverriddenDefault().foo(), is("overridden"));
    }

    private interface OriginalDefault {
        default String foo() {
            return "original";
        }
    }

    private interface OverriddenDefault extends OriginalDefault {
        @Override
        default String foo() {
            return "overridden";
        }
    }

    private class ImplementsOriginal implements OriginalDefault {
    }

    private class ImplementsOriginalAndOverriddenDefault implements OriginalDefault, OverriddenDefault {
    }

    private class ImplementsOverriddenAndOriginalDefault implements OverriddenDefault, OriginalDefault {
    }

    private class ExtendsImplementsOriginalAndImplementsOverriddenDefault extends ImplementsOriginal implements OverriddenDefault {
    }


    // Bridge Methods

    @Test
    public void default_method_type_refined_in_child_interface() {
        RefineChild child = new RefineChild() {
            @Override
            public String foo() {
                return "refined";
            }
        };
        assertThat("direct call", child.foo(), is("refined"));
        assertThat("bridged call", ((RefineParent) child).foo(), is((Object) "refined"));
    }

    @Test
    public void default_method_type_refined_in_implementing_class() {
        class C implements RefineParent {
            @Override
            public String foo() {
                return "refined";
            }
        }
        C obj = new C();
        assertThat("direct call", obj.foo(), is("refined"));
        assertThat("bridged call", ((RefineParent) obj).foo(), is((Object) "refined"));
    }

    private interface RefineParent {
        default Object foo() {
            return "original";
        }
    }

    private interface RefineChild extends RefineParent {
        @Override
        String foo();
    }


    @Test
    public void default_method_argument_type_refined_in_child_interface() {
        RefineArgChild child = new RefineArgChild() {
        };
        assertThat("direct call", child.foo("42"), is("refined 42"));
        assertThat("bridged call", ((RefineArgParent<String>) child).foo("42"), is((Object) "refined 42"));
    }

    @Test
    public void default_method_argument_type_refined_in_implementing_class() {
        class C implements RefineArgParent<String> {
            @Override
            public String foo(String arg) {
                return "refined " + arg;
            }
        }
        C obj = new C();
        assertThat("direct call", obj.foo("42"), is("refined 42"));
        assertThat("bridged call", ((RefineArgParent<String>) obj).foo("42"), is((Object) "refined 42"));
    }

    private interface RefineArgParent<T> {
        default String foo(T arg) {
            return "original " + arg;
        }
    }

    private interface RefineArgChild extends RefineArgParent<String> {
        @Override
        default String foo(String arg) {
            return "refined " + arg;
        }
    }


    @Test
    public void default_method_type_refined_and_overridden_in_child_interface() {
        OverrideRefineChild child = new OverrideRefineChild() {
        };
        assertThat("direct call", child.foo(), is("overridden and refined"));
        assertThat("bridged call", ((OverrideRefineParent) child).foo(), is((Object) "overridden and refined"));
    }

    private interface OverrideRefineParent {
        default Object foo() {
            return "original";
        }
    }

    private interface OverrideRefineChild extends OverrideRefineParent {
        @Override
        default String foo() {
            return "overridden and refined";
        }
    }


    // Primitive Types & Void

    @Test
    public void default_methods_of_primitive_type() {
        Primitives p = new Primitives() {
        };
        assertThat("boolean", p.getBoolean(), is(true));
        assertThat("byte", p.getByte(), is((byte) 2));
        assertThat("short", p.getShort(), is((short) 3));
        assertThat("int", p.getInt(), is(4));
        assertThat("long", p.getLong(), is(5L));
        assertThat("float", p.getFloat(), is(6.0f));
        assertThat("double", p.getDouble(), is(7.0));
        assertThat("char", p.getChar(), is('a'));
    }

    private interface Primitives {
        default boolean getBoolean() {
            return true;
        }

        default byte getByte() {
            return 2;
        }

        default short getShort() {
            return 3;
        }

        default int getInt() {
            return 4;
        }

        default long getLong() {
            return 5L;
        }

        default float getFloat() {
            return 6.0f;
        }

        default double getDouble() {
            return 7.0;
        }

        default char getChar() {
            return 'a';
        }
    }


    @Test
    public void default_methods_of_void_type() {
        modifiedByVoidMethod = 1;
        Voids v = new Voids() {
        };
        v.run();
        assertThat(modifiedByVoidMethod, is(2));
    }

    private static int modifiedByVoidMethod;

    private interface Voids {
        default void run() {
            modifiedByVoidMethod++;
        }
    }


    @Test
    public void default_methods_with_primitive_arguments() {
        PrimitiveArgs p = new PrimitiveArgs() {
        };
        assertThat(p.sum(true, (byte) 2, (short) 3, 4, 5, 6, 7, (char) 8), is(36));
    }

    private interface PrimitiveArgs {
        default int sum(boolean bool, byte b, short s, int i, long l, float f, double d, char c) {
            return (int) ((bool ? 1 : 0) + b + s + i + l + f + d + c);
        }
    }


    // Calling Super

    @Test
    public void default_methods_calling_super() {
        SuperCallChild child = new SuperCallChild() {
        };
        assertThat(child.callSuper(), is(11));
    }

    @Test
    public void default_methods_called_with_super() {
        class C implements SuperCallChild {
            @Override
            public int callSuper() {
                return 100 + SuperCallChild.super.callSuper();
            }

            public int siblingCallingSuper() {
                return 1000 + SuperCallChild.super.callSuper();
            }
        }
        assertThat(new C().callSuper(), is(111));
        assertThat(new C().siblingCallingSuper(), is(1011));
    }

    private interface SuperCallParent {
        default int callSuper() {
            return 1;
        }
    }

    private interface SuperCallChild extends SuperCallParent {
        @Override
        default int callSuper() {
            return 10 + SuperCallParent.super.callSuper();
        }
    }


    @Test
    public void inheriting_unrelated_default_methods() {
        class C implements Conflict1, Conflict2 {
            @Override
            public String conflict() {
                return Conflict1.super.conflict() + Conflict2.super.conflict();
            }
        }
        assertThat(new C().conflict(), is("ab"));
    }

    private interface Conflict1 {
        default String conflict() {
            return "a";
        }
    }

    private interface Conflict2 {
        default String conflict() {
            return "b";
        }
    }


    // Misc

    @Test
    public void default_methods_calling_other_interface_methods() {
        CallOtherMethods obj = new CallOtherMethods() {
            @Override
            public int foo() {
                return 2;
            }
        };
        assertThat(obj.callsFoo(), is(12));
    }

    private interface CallOtherMethods {
        int foo();

        default int callsFoo() {
            return foo() + 10;
        }
    }

    /**
     * Backporting default methods should not interact badly with backporting lambdas.
     */
    @Test
    public void lambdas_with_default_methods() {
        CallOtherMethods lambda = () -> 2;
        assertThat(lambda.foo(), is(2));
        assertThat(lambda.callsFoo(), is(12));
    }

    @Test
    public void default_methods_with_lambdas() throws Exception {
        UsesLambdas obj = new UsesLambdas() {
        };
        assertThat(obj.stateless().call(), is("foo"));
    }

    @Test
    public void default_methods_with_lambdas_that_capture_this() throws Exception {
        UsesLambdas obj = new UsesLambdas() {
        };
        assertThat(obj.captureThis().call(), is("foo"));
    }

    private interface UsesLambdas {
        default Callable<String> stateless() {
            return () -> "foo";
        }

        default Callable<String> captureThis() {
            return () -> stateless().call();
        }
    }

    /**
     * Lambdas which capture this in default methods will generate the lambda implementation
     * method as a private <em>instance</em> method. We must avoid copying those methods to
     * the interface implementers as if they were default methods.
     */
    @Test
    public void default_methods_with_lambdas_in_another_package() throws Exception {
        assumeThat(SystemUtils.JAVA_VERSION_FLOAT, is(lessThan(1.8f)));

        UsesLambdasInAnotherPackage obj = new UsesLambdasInAnotherPackage() {
        };
        assertThat(obj.stateless().call(), is("foo"));
        assertThat(obj.captureThis().call(), is("foo"));
        assertThat("should contain only delegates to the two default methods",
                obj.getClass().getDeclaredMethods(), arrayWithSize(2));
    }

    /**
     * Though we use {@link InMainSources}, because the Retrolambda Maven plugin
     * processes the main sources separately from the test sources, the effect is
     * the same as if they were in another module.
     */
    @Test
    public void calling_default_methods_from_another_module_through_interface() {
        InMainSources.Interface implementer = new InMainSources.Implementer();
        assertThat(implementer.defaultMethod(), is("default"));

        InMainSources.Interface overrider = new InMainSources.Overrider();
        assertThat(overrider.defaultMethod(), is("overridden"));
    }

    /**
     * Fixes issue of the generated delegate methods being marked as synthetic,
     * in which case the Java compiler causes "error: cannot find symbol"
     * for direct calls to those methods.
     */
    @Test
    public void calling_default_methods_from_another_module_through_class() {
        InMainSources.Implementer implementer = new InMainSources.Implementer();
        assertThat(implementer.defaultMethod(), is("default"));

        InMainSources.Overrider overrider = new InMainSources.Overrider();
        assertThat(overrider.defaultMethod(), is("overridden"));
    }


    /**
     * We're unable to backport default methods if we cannot modify the interface,
     * e.g. if it's part of the standard library or a third-party library.
     */
    @Test
    public void default_methods_of_library_interfaces_are_ignored_silently() throws Exception {
        @SuppressWarnings("unchecked") Iterator<String> dummy = mock(Iterator.class);

        // the Iterable interface has default methods in Java 8, but that
        // should not prevent us from using it in previous Java versions
        Iterable<String> it = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return dummy;
            }
        };

        assertThat("interface should work as usual", it.iterator(), is(dummy));
        assertThat("should not copy default methods from library interfaces",
                it.getClass().getDeclaredMethods(), arrayWithSize(1));
    }

    @Test
    public void trying_to_use_default_methods_of_library_interfaces_causes_NoSuchMethodError() {
        assumeThat(SystemUtils.JAVA_VERSION_FLOAT, is(lessThan(1.8f)));

        class C implements Iterable<String> {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }
        }

        thrown.expect(NoSuchMethodError.class);
        thrown.expectMessage("spliterator");
        // Called directly on the class (invokevirtual) instead of the interface (invokeinterface),
        // to make sure that no method was inserted to the class (in which case this call would not fail)
        new C().spliterator();
    }

    /**
     * A naive method for removing method bodies would easily also remove their annotations,
     * because in ASM method annotations are expressed as calls on the MethodVisitor.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void keeps_annotations_on_interface_methods() throws Exception {
        assertThat("interface", AnnotatedInterface.class.getAnnotations(),
                arrayContaining(someAnnotation(1)));

        assertThat("abstract method", AnnotatedInterface.class.getMethod("annotatedAbstractMethod").getAnnotations(),
                arrayContaining(someAnnotation(2)));

        assertThat("default method", AnnotatedInterface.class.getMethod("annotatedDefaultMethod").getAnnotations(),
                arrayContaining(someAnnotation(3)));

        assumeThat(SystemUtils.JAVA_VERSION_FLOAT, is(lessThan(1.8f)));
        assertThat("static method", companionOf(AnnotatedInterface.class).getMethod("annotatedStaticMethod").getAnnotations(),
                arrayContaining(someAnnotation(4)));
    }

    @SomeAnnotation(1)
    private interface AnnotatedInterface {

        @SomeAnnotation(2)
        void annotatedAbstractMethod();

        @SomeAnnotation(3)
        default void annotatedDefaultMethod() {
        }

        @SomeAnnotation(4)
        static void annotatedStaticMethod() {
        }
    }

    @Retention(value = RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
        int value();
    }

    private static Matcher<Annotation> someAnnotation(int value) {
        return new TypeSafeMatcher<Annotation>() {
            @Override
            protected boolean matchesSafely(Annotation item) {
                return item instanceof SomeAnnotation && ((SomeAnnotation) item).value() == value;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("@SomeAnnotation(" + value + ")");
            }
        };
    }
}
