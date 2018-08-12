// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import com.google.common.io.ByteStreams;
import net.orfjackal.retrolambda.interfaces.*;
import org.junit.*;
import org.objectweb.asm.Type;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("UnusedDeclaration")
public class ClassAnalyzerTest {

    private final ClassAnalyzer analyzer = new ClassAnalyzer();

    @Test
    public void lists_interfaces_and_classes_separately() {
        analyze(Interface.class,
                InterfaceImplementer.class);

        assertThat("interfaces", getInterfaces(), is(classList(Interface.class)));
        assertThat("classes", getClasses(), is(classList(InterfaceImplementer.class)));
    }


    // Method inheritance

    @Test
    public void abstract_interface_method_inherited_and_implemented() {
        analyze(Interface.class,
                ChildInterface.class,
                InterfaceImplementer.class);

        assertThat("original", analyzer.getMethods(Type.getType(Interface.class)),
                hasItem(new MethodInfo("abstractMethod", "()V", Interface.class, new MethodKind.Abstract())));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(ChildInterface.class)),
                hasItem(new MethodInfo("abstractMethod", "()V", Interface.class, new MethodKind.Abstract())));

        assertThat("implements", analyzer.getMethods(Type.getType(InterfaceImplementer.class)),
                hasItem(new MethodInfo("abstractMethod", "()V", InterfaceImplementer.class, new MethodKind.Implemented())));
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


    @Test
    public void interface_method_types() {
        analyze(InterfaceMethodTypes.class);

        assertThat(analyzer.getMethods(Type.getType(InterfaceMethodTypes.class)),
                hasItems(
                        new MethodInfo("abstractMethod", "()V", InterfaceMethodTypes.class, new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", InterfaceMethodTypes.class, new MethodKind.Default(
                                new MethodRef(H_INVOKESTATIC, InterfaceMethodTypes$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$InterfaceMethodTypes;)V")))));
    }

    @Test
    public void class_method_types() {
        analyze(ClassMethodTypes.class);

        // An abstract instance method takes precedence over a default method,
        // so we handle abstract instance methods the same way as concrete instance methods.
        assertThat(analyzer.getMethods(Type.getType(ClassMethodTypes.class)),
                hasItems(
                        new MethodInfo("abstractMethod", "()V", ClassMethodTypes.class, new MethodKind.Implemented()),
                        new MethodInfo("instanceMethod", "()V", ClassMethodTypes.class, new MethodKind.Implemented())));
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


    @Test
    public void default_method_overridden_and_abstracted() {
        analyze(HasDefaultMethods.class,
                DoesNotOverrideDefaultMethods.class,
                OverridesDefaultMethods.class,
                AbstractsDefaultMethods.class);

        assertThat("original", analyzer.getMethods(Type.getType(HasDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", HasDefaultMethods.class, new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", HasDefaultMethods.class, new MethodKind.Default(
                                new MethodRef(H_INVOKESTATIC, HasDefaultMethods$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$HasDefaultMethods;)V")))));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(DoesNotOverrideDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", HasDefaultMethods.class, new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", HasDefaultMethods.class, new MethodKind.Default(
                                new MethodRef(H_INVOKESTATIC, HasDefaultMethods$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$HasDefaultMethods;)V")))));

        assertThat("changes default impl", analyzer.getMethods(Type.getType(OverridesDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", HasDefaultMethods.class, new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", OverridesDefaultMethods.class, new MethodKind.Default(
                                new MethodRef(H_INVOKESTATIC, OverridesDefaultMethods$.class, "defaultMethod", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$OverridesDefaultMethods;)V")))));

        assertThat("makes abstract", analyzer.getMethods(Type.getType(AbstractsDefaultMethods.class)),
                containsInAnyOrder(
                        new MethodInfo("abstractMethod", "()V", HasDefaultMethods.class, new MethodKind.Abstract()),
                        new MethodInfo("defaultMethod", "()V", AbstractsDefaultMethods.class, new MethodKind.Abstract())));
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


    @Test
    public void superclass_inheritance() {
        analyze(BaseClass.class,
                ChildClass.class);

        assertThat("original", analyzer.getMethods(Type.getType(BaseClass.class)),
                hasItem(
                        new MethodInfo("baseMethod", "()V", BaseClass.class, new MethodKind.Implemented())));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(ChildClass.class)),
                hasItem(
                        new MethodInfo("baseMethod", "()V", BaseClass.class, new MethodKind.Implemented())));
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
                hasItem(
                        new MethodInfo("foo", "()V", DefaultMethods.class, new MethodKind.Default(
                                new MethodRef(H_INVOKESTATIC, DefaultMethods$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$DefaultMethods;)V")))));

        assertThat("inherits unchanged", analyzer.getMethods(Type.getType(InheritsDefault.class)),
                hasItem(
                        new MethodInfo("foo", "()V", DefaultMethods.class, new MethodKind.Default(
                                new MethodRef(H_INVOKESTATIC, DefaultMethods$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$DefaultMethods;)V")))));

        assertThat("overrides", analyzer.getMethods(Type.getType(OverridesDefault.class)),
                hasItem(
                        new MethodInfo("foo", "()V", OverridesDefault.class, new MethodKind.Implemented())));

        assertThat("inherits overridden", analyzer.getMethods(Type.getType(InheritsOverridesDefault.class)),
                hasItem(
                        new MethodInfo("foo", "()V", OverridesDefault.class, new MethodKind.Implemented())));

        assertThat("inherits overridden", analyzer.getMethods(Type.getType(InheritsOverridesDefaultAndDirectlyImplements.class)),
                hasItem(
                        new MethodInfo("foo", "()V", OverridesDefault.class, new MethodKind.Implemented())));
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


    @Test
    public void inheriting_same_default_methods_through_many_parent_interfaces() {
        analyze(SuperOriginal.class,
                SuperOverridden.class,
                InheritsOriginal.class,
                InheritsOverridden.class,
                InheritsOverriddenAndOriginal.class,
                InheritsOriginalAndOverridden.class);

        MethodInfo original = new MethodInfo("foo", "()V", SuperOriginal.class, new MethodKind.Default(
                new MethodRef(H_INVOKESTATIC, SuperOriginal$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$SuperOriginal;)V")));
        MethodInfo overridden = new MethodInfo("foo", "()V", SuperOverridden.class, new MethodKind.Default(
                new MethodRef(H_INVOKESTATIC, SuperOverridden$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$SuperOverridden;)V")));

        assertThat("inherits original", analyzer.getMethods(Type.getType(InheritsOriginal.class)),
                containsInAnyOrder(original));
        assertThat("inherits overridden", analyzer.getMethods(Type.getType(InheritsOverridden.class)),
                containsInAnyOrder(overridden));
        assertThat("inherits overridden and original", analyzer.getMethods(Type.getType(InheritsOverriddenAndOriginal.class)),
                containsInAnyOrder(overridden));
        assertThat("inherits original and overridden", analyzer.getMethods(Type.getType(InheritsOriginalAndOverridden.class)),
                containsInAnyOrder(overridden));
    }

    private interface SuperOriginal {
        default void foo() {
        }
    }

    private interface SuperOriginal$ {
    }

    private interface SuperOverridden extends SuperOriginal {
        @Override
        default void foo() {
        }
    }

    private interface SuperOverridden$ {
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
        analyze(OriginalDefault.class,
                OverriddenDefault.class,
                ImplementsOriginal.class,
                ImplementsOriginalAndOverriddenDefault.class,
                ImplementsOverriddenAndOriginalDefault.class,
                ExtendsImplementsOriginalAndImplementsOverriddenDefault.class);

        MethodInfo original = new MethodInfo("foo", "()V", OriginalDefault.class, new MethodKind.Default(
                new MethodRef(H_INVOKESTATIC, OriginalDefault$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$OriginalDefault;)V")));
        MethodInfo overridden = new MethodInfo("foo", "()V", OverriddenDefault.class, new MethodKind.Default(
                new MethodRef(H_INVOKESTATIC, OverriddenDefault$.class, "foo", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$OverriddenDefault;)V")));

        assertThat("implements original", analyzer.getMethods(Type.getType(ImplementsOriginal.class)),
                hasItem(original));
        assertThat("implements original and overridden", analyzer.getMethods(Type.getType(ImplementsOriginalAndOverriddenDefault.class)),
                hasItem(overridden));
        assertThat("implements overridden and original", analyzer.getMethods(Type.getType(ImplementsOverriddenAndOriginalDefault.class)),
                hasItem(overridden));
        assertThat("extends implementor of original and implements overridden", analyzer.getMethods(Type.getType(ExtendsImplementsOriginalAndImplementsOverriddenDefault.class)),
                hasItem(overridden));
    }

    private interface OriginalDefault {
        default void foo() {
        }
    }

    private interface OriginalDefault$ {
    }

    private interface OverriddenDefault extends OriginalDefault {
        @Override
        default void foo() {
        }
    }

    private interface OverriddenDefault$ {
    }

    private class ImplementsOriginal implements OriginalDefault {
    }

    private class ImplementsOriginalAndOverriddenDefault implements OriginalDefault, OverriddenDefault {
    }

    private class ImplementsOverriddenAndOriginalDefault implements OverriddenDefault, OriginalDefault {
    }

    private class ExtendsImplementsOriginalAndImplementsOverriddenDefault extends ImplementsOriginal implements OverriddenDefault {
    }


    @Test
    public void default_methods_with_lambdas() {
        analyze(UsesLambdas.class,
                ImplementsUsesLambdas.class);

        MethodInfo stateless = new MethodInfo("stateless", "()Ljava/util/concurrent/Callable;", UsesLambdas.class, new MethodKind.Default(
                new MethodRef(H_INVOKESTATIC, UsesLambdas$.class, "stateless", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$UsesLambdas;)Ljava/util/concurrent/Callable;")));
        MethodInfo captureThis = new MethodInfo("captureThis", "()Ljava/util/concurrent/Callable;", UsesLambdas.class, new MethodKind.Default(
                new MethodRef(H_INVOKESTATIC, UsesLambdas$.class, "captureThis", "(Lnet/orfjackal/retrolambda/ClassAnalyzerTest$UsesLambdas;)Ljava/util/concurrent/Callable;")));

        assertThat("does not copy instance lambda impl methods to implementers",
                analyzer.getMethods(Type.getType(ImplementsUsesLambdas.class)),
                hasItems(stateless, captureThis));
    }

    private interface UsesLambdas {
        default Callable<String> stateless() {
            return () -> "foo";
        }

        default Callable<String> captureThis() {
            return () -> stateless().call();
        }
    }

    private interface UsesLambdas$ {
    }

    private class ImplementsUsesLambdas implements UsesLambdas {
    }


    // Method relocations

    @Test
    public void abstract_methods_on_interfaces_are_not_relocated() {
        analyze(InterfaceMethodTypes.class);

        MethodRef source = new MethodRef(H_INVOKEINTERFACE, InterfaceMethodTypes.class, "abstractMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(source));
    }

    @Test
    public void default_methods_on_interfaces_are_not_relocated() {
        analyze(InterfaceMethodTypes.class);

        MethodRef source = new MethodRef(H_INVOKEINTERFACE, InterfaceMethodTypes.class, "defaultMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(source));
    }

    @Test
    public void static_methods_on_interfaces_are_relocated_to_companion_classes() {
        analyze(InterfaceMethodTypes.class);

        MethodRef source = new MethodRef(H_INVOKESTATIC, InterfaceMethodTypes.class, "staticMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(new MethodRef(H_INVOKESTATIC, InterfaceMethodTypes$.class, "staticMethod", voidMethod())));
    }

    @Test
    public void static_methods_on_classes_are_not_relocated() {
        analyze(ClassMethodTypes.class);

        MethodRef source = new MethodRef(H_INVOKESTATIC, ClassMethodTypes.class, "staticMethod", voidMethod());
        MethodRef target = analyzer.getMethodCallTarget(source);

        assertThat(target, is(source));
    }


    // Default method implementations

    @Test
    public void abstract_methods_have_no_implementation() {
        analyze(HasDefaultMethods.class);

        MethodRef method = new MethodRef(H_INVOKEINTERFACE, HasDefaultMethods.class, "abstractMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(nullValue()));
    }

    @Test
    public void default_method_implementation_is_moved_to_companion_class() {
        analyze(HasDefaultMethods.class);

        MethodRef method = new MethodRef(H_INVOKEINTERFACE, HasDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(new MethodRef(H_INVOKESTATIC, HasDefaultMethods$.class, "defaultMethod", voidMethod(HasDefaultMethods.class))));
    }

    @Test
    public void default_method_implementations_are_inherited_from_parent_interface() {
        analyze(HasDefaultMethods.class,
                DoesNotOverrideDefaultMethods.class);

        MethodRef method = new MethodRef(H_INVOKEINTERFACE, DoesNotOverrideDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(new MethodRef(H_INVOKESTATIC, HasDefaultMethods$.class, "defaultMethod", voidMethod(HasDefaultMethods.class))));
    }

    @Test
    public void overridden_default_method_implementation_is_moved_to_companion_class() {
        analyze(HasDefaultMethods.class,
                OverridesDefaultMethods.class);

        MethodRef method = new MethodRef(H_INVOKEINTERFACE, OverridesDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(new MethodRef(H_INVOKESTATIC, OverridesDefaultMethods$.class, "defaultMethod", voidMethod(OverridesDefaultMethods.class))));
    }

    @Test
    public void abstracted_default_method_implementations_are_again_abstract() {
        analyze(HasDefaultMethods.class,
                AbstractsDefaultMethods.class);

        MethodRef method = new MethodRef(H_INVOKEINTERFACE, AbstractsDefaultMethods.class, "defaultMethod", voidMethod());
        MethodRef impl = analyzer.getMethodDefaultImplementation(method);

        assertThat(impl, is(nullValue()));
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
            analyzer.analyze(bytecode, false);
        }
    }

    private List<Class<?>> getInterfaces() {
        return infosToClasses(analyzer.getInterfaces());
    }

    private List<Class<?>> getClasses() {
        return infosToClasses(analyzer.getClasses());
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
                .map(ClassAnalyzerTest::toClass)
                .collect(toList());
    }

    private static List<Class<?>> typesToClasses(List<Type> types) {
        return types.stream()
                .map(ClassAnalyzerTest::toClass)
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
