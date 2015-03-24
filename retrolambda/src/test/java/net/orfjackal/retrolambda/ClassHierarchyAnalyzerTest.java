// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import com.google.common.io.ByteStreams;
import net.orfjackal.retrolambda.interfaces.*;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("UnusedDeclaration")
public class ClassHierarchyAnalyzerTest {

    private final ClassHierarchyAnalyzer analyzer = new ClassHierarchyAnalyzer();

    @Test
    public void finds_interfaces_and_classes_separately() {
        analyze(Interface.class,
                InterfaceImplementer.class);

        assertThat("interfaces", getInterfaces(), is(classList(Interface.class)));
        assertThat("classes", getClasses(), is(classList(InterfaceImplementer.class)));
    }

    @Test
    public void finds_implemented_interfaces() {
        analyze(Interface.class,
                ChildInterface.class,
                InterfaceImplementer.class);

        assertThat("Interface", getInterfacesOf(Interface.class), is(empty()));
        assertThat("ChildInterface", getInterfacesOf(ChildInterface.class), is(classList(Interface.class)));
        assertThat("InterfaceImplementer", getInterfacesOf(InterfaceImplementer.class), is(classList(Interface.class)));
    }

    @Test
    public void finds_interface_methods() {
        analyze(InterfaceMethodTypes.class);

        assertThat(analyzer.getInterfaceMethods(Type.getType(InterfaceMethodTypes.class)),
                containsInAnyOrder(
                        new MethodRef(InterfaceMethodTypes.class, "abstractMethod", voidMethod()),
                        new MethodRef(InterfaceMethodTypes.class, "defaultMethod", voidMethod()))); // all but staticMethod
    }

    @Test
    public void finds_inherited_interface_methods() {
        analyze(ChildInterface.class,
                Interface.class);

        assertThat(analyzer.getInterfaceMethods(Type.getType(ChildInterface.class)),
                containsInAnyOrder(
                        new MethodRef(ChildInterface.class, "abstractMethod", voidMethod())));
    }

    @Test
    public void does_not_find_interface_methods_of_not_analyzed_interfaces() {
        assertThat(analyzer.getInterfaceMethods(Type.getType(Interface.class)), is(empty()));
    }

    private interface Interface {
        void abstractMethod();
    }

    private interface ChildInterface extends Interface {
    }

    private class InterfaceImplementer implements Interface {
        @Override
        public void abstractMethod() {
        }
    }


    // Method inheritance

    @Test
    public void abstract_interface_method_inherited_and_implemented() {
        analyze(Interface.class,
                ChildInterface.class,
                InterfaceImplementer.class);

        assertThat("original", analyzer.getMethods(Type.getType(Interface.class)),
                containsInAnyOrder(new MethodInfo("abstractMethod", "()V", new MethodKind.Abstract())));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(ChildInterface.class)),
                containsInAnyOrder(new MethodInfo("abstractMethod", "()V", new MethodKind.Abstract())));

        assertThat("implements", analyzer.getMethods(Type.getType(InterfaceImplementer.class)),
                containsInAnyOrder(new MethodInfo("abstractMethod", "()V", new MethodKind.Concrete())));
    }

    @Test
    public void interface_method_types() {
        analyze(InterfaceMethodTypes.class);

        assertThat(analyzer.getMethods(Type.getType(InterfaceMethodTypes.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", new MethodKind.Default(
                                new MethodRef(InterfaceMethodTypes$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassHierarchyAnalyzerTest$InterfaceMethodTypes;)V")))));
    }

    @Test
    public void class_method_types() {
        analyze(ClassMethodTypes.class);

        // TODO: make a difference between abstract and concrete instance methods?
        // An abstract instance method will probably take precedence over a default method,
        // so our algorithm might require abstract instance methods to be considered same as concrete.
        assertThat(analyzer.getMethods(Type.getType(ClassMethodTypes.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", new MethodKind.Concrete()),
                        new MethodInfo("instanceMethod", "()V", new MethodKind.Concrete())));
    }

    @Test
    public void default_method_overridden_and_abstracted() {
        analyze(HasDefaultMethods.class,
                DoesNotOverrideDefaultMethods.class,
                OverridesDefaultMethods.class,
                AbstractsDefaultMethods.class);

        assertThat("original", analyzer.getMethods(Type.getType(HasDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", new MethodKind.Default(
                                new MethodRef(HasDefaultMethods$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassHierarchyAnalyzerTest$HasDefaultMethods;)V")))));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(DoesNotOverrideDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", new MethodKind.Default(
                                new MethodRef(HasDefaultMethods$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassHierarchyAnalyzerTest$HasDefaultMethods;)V")))));

        assertThat("changes default impl", analyzer.getMethods(Type.getType(OverridesDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", new MethodKind.Default(
                                new MethodRef(OverridesDefaultMethods$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassHierarchyAnalyzerTest$OverridesDefaultMethods;)V")))));

        assertThat("makes abstract", analyzer.getMethods(Type.getType(AbstractsDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", new MethodKind.Abstract())));
    }

    @Test
    public void superclass_inheritance() {
        analyze(BaseClass.class,
                ChildClass.class);

        assertThat("original", analyzer.getMethods(Type.getType(BaseClass.class)),
                containsInAnyOrder(
                        new MethodInfo("baseMethod", "()V", new MethodKind.Concrete())));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(ChildClass.class)),
                containsInAnyOrder(
                        new MethodInfo("baseMethod", "()V", new MethodKind.Concrete())));
    }

    private class BaseClass {
        void baseMethod() {
        }
    }

    private class ChildClass extends BaseClass {
    }

    @Test
    public void overriding_default_methods() {
        analyze(DefaultMethods.class,
                InheritsDefault.class,
                OverridesDefault.class,
                InheritsOverridesDefault.class,
                InheritsOverridesDefaultAndDirectlyImplements.class);

        assertThat("original", analyzer.getMethods(Type.getType(DefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("foo", "()V", new MethodKind.Default(
                                new MethodRef(DefaultMethods$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassHierarchyAnalyzerTest$DefaultMethods;)V")))));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(InheritsDefault.class)),
                containsInAnyOrder(
                        new MethodInfo("foo", "()V", new MethodKind.Default(
                                new MethodRef(DefaultMethods$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassHierarchyAnalyzerTest$DefaultMethods;)V")))));

        assertThat("overrides", analyzer.getMethods(Type.getType(OverridesDefault.class)),
                containsInAnyOrder(
                        new MethodInfo("foo", "()V", new MethodKind.Concrete())));

        assertThat("inherits overridden", analyzer.getMethods(Type.getType(InheritsOverridesDefault.class)),
                containsInAnyOrder(
                        new MethodInfo("foo", "()V", new MethodKind.Concrete())));

        assertThat("inherits overridden", analyzer.getMethods(Type.getType(InheritsOverridesDefaultAndDirectlyImplements.class)),
                containsInAnyOrder(
                        new MethodInfo("foo", "()V", new MethodKind.Concrete())));
    }

    private interface DefaultMethods {
        default void foo() {
        }
    }

    private interface DefaultMethods$ {
    }

    private class InheritsDefault implements DefaultMethods {
    }

    private class OverridesDefault implements DefaultMethods {
        @Override
        public void foo() {
        }
    }

    class InheritsOverridesDefault extends OverridesDefault {
    }

    class InheritsOverridesDefaultAndDirectlyImplements extends OverridesDefault implements DefaultMethods {
    }


    // TODO: edge cases from e2e tests


    // Method relocations

    @Test
    public void abstract_methods_on_interfaces_are_not_relocated() {
        analyze(InterfaceMethodTypes.class);

        MethodRef source = new MethodRef(InterfaceMethodTypes.class, "abstractMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(source));
    }

    @Test
    public void default_methods_on_interfaces_are_not_relocated() {
        analyze(InterfaceMethodTypes.class);

        MethodRef source = new MethodRef(InterfaceMethodTypes.class, "defaultMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(source));
    }

    @Test
    public void static_methods_on_interfaces_are_relocated_to_companion_classes() {
        analyze(InterfaceMethodTypes.class);

        MethodRef source = new MethodRef(InterfaceMethodTypes.class, "staticMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(new MethodRef(InterfaceMethodTypes$.class, "staticMethod", voidMethod())));
    }

    @Test
    public void static_methods_on_classes_are_not_relocated() {
        analyze(ClassMethodTypes.class);

        MethodRef source = new MethodRef(ClassMethodTypes.class, "staticMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(source));
    }

    private interface InterfaceMethodTypes {
        void abstractMethod();

        default void defaultMethod() {
        }

        static void staticMethod() {
        }
    }

    private interface InterfaceMethodTypes$ {
    }

    private static abstract class ClassMethodTypes {
        public abstract void abstractMethod();

        public void instanceMethod() {
        }

        public static void staticMethod() {
        }
    }


    // Default method implementations

    @Test
    public void abstract_methods_have_no_implementation() {
        analyze(HasDefaultMethods.class);

        MethodRef method = new MethodRef(HasDefaultMethods.class, "abstractMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(nullValue()));
    }

    @Test
    public void default_method_implementation_is_moved_to_companion_class() {
        analyze(HasDefaultMethods.class);

        MethodRef method = new MethodRef(HasDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(new MethodRef(HasDefaultMethods$.class, "defaultMethod", voidMethod(HasDefaultMethods.class))));
    }

    @Test
    public void default_method_implementations_are_inherited_from_parent_interface() {
        analyze(HasDefaultMethods.class,
                DoesNotOverrideDefaultMethods.class);

        MethodRef method = new MethodRef(DoesNotOverrideDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(new MethodRef(HasDefaultMethods$.class, "defaultMethod", voidMethod(HasDefaultMethods.class))));
    }

    @Test
    public void overridden_default_method_implementation_is_moved_to_companion_class() {
        analyze(HasDefaultMethods.class,
                OverridesDefaultMethods.class);

        MethodRef method = new MethodRef(OverridesDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(new MethodRef(OverridesDefaultMethods$.class, "defaultMethod", voidMethod(OverridesDefaultMethods.class))));
    }

    @Test
    public void abstracted_default_method_implementations_are_again_abstract() {
        analyze(HasDefaultMethods.class,
                AbstractsDefaultMethods.class);

        MethodRef method = new MethodRef(AbstractsDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(nullValue()));
    }

    private interface HasDefaultMethods {
        void abstractMethod();

        default void defaultMethod() {
        }
    }

    private interface HasDefaultMethods$ {
    }

    private interface DoesNotOverrideDefaultMethods extends HasDefaultMethods {
    }

    private interface OverridesDefaultMethods extends HasDefaultMethods {
        @Override
        default void defaultMethod() {
        }
    }

    private interface OverridesDefaultMethods$ {
    }

    private interface AbstractsDefaultMethods extends HasDefaultMethods {
        @Override
        void defaultMethod();
    }


    // Companion class

    @Test
    public void companion_class_is_needed_when_methods_are_moved_there() {
        analyze(Interface.class,
                InterfaceMethodTypes.class,
                HasDefaultMethods.class,
                ClassMethodTypes.class);

        assertThat("Interface", analyzer.getCompanionClass(Type.getType(Interface.class)), is(Optional.empty()));
        assertThat("InterfaceMethodTypes", analyzer.getCompanionClass(Type.getType(InterfaceMethodTypes.class)), is(Optional.of(Type.getType(InterfaceMethodTypes$.class))));
        assertThat("HasDefaultMethods", analyzer.getCompanionClass(Type.getType(HasDefaultMethods.class)), is(Optional.of(Type.getType(HasDefaultMethods$.class))));
        assertThat("ClassMethodTypes", analyzer.getCompanionClass(Type.getType(ClassMethodTypes.class)), is(Optional.empty()));
    }


    // API wrappers

    private void analyze(Class<?>... classes) {
        List<Class<?>> inAnyOrder = new ArrayList<>(Arrays.asList(classes));
        Collections.shuffle(inAnyOrder);
        for (Class<?> clazz : inAnyOrder) {
            byte[] bytecode = readBytecode(clazz);
            analyzer.analyze(bytecode);
        }
    }

    private List<Class<?>> getInterfaces() {
        return infosToClasses(analyzer.getInterfaces());
    }

    private List<Class<?>> getClasses() {
        return infosToClasses(analyzer.getClasses());
    }

    private List<Class<?>> getInterfacesOf(Class<?> clazz) {
        return typesToClasses(analyzer.getInterfacesOf(Type.getType(clazz)));
    }


    // other helpers

    private static String voidMethod(Class<?>... argumentTypes) {
        return Type.getMethodDescriptor(Type.VOID_TYPE,
                Stream.of(argumentTypes)
                        .map(Type::getType)
                        .toArray(Type[]::new));
    }

    private static List<Class<?>> infosToClasses(List<ClassInfo> classes) {
        return classes.stream()
                .map(ClassHierarchyAnalyzerTest::toClass)
                .collect(toList());
    }

    private static List<Class<?>> typesToClasses(List<Type> types) {
        return types.stream()
                .map(ClassHierarchyAnalyzerTest::toClass)
                .collect(toList());
    }

    private static List<Class<?>> classList(Class<?>... aClass) {
        return asList(aClass);
    }

    private static Class<?> toClass(ClassInfo c) {
        try {
            return Class.forName(c.type.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> toClass(Type type) {
        try {
            return Class.forName(type.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readBytecode(Class<?> clazz) {
        try (InputStream in = clazz.getResourceAsStream("/" + Type.getType(clazz).getInternalName() + ".class")) {
            return ByteStreams.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
