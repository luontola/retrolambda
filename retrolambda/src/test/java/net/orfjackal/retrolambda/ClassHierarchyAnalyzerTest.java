// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.io.*;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ClassHierarchyAnalyzerTest {

    private final ClassHierarchyAnalyzer analyzer = new ClassHierarchyAnalyzer();

    @Test
    public void separates_interfaces_from_classes() {
        analyze(Interface.class);
        analyze(InterfaceImplementer.class);

        assertThat(getInterfaces(), is(classList(Interface.class)));
        assertThat(getClasses(), is(classList(InterfaceImplementer.class)));
    }

    @Test
    public void no_parent_interfaces() {
        analyze(Interface.class);

        assertThat(getInterfacesOf(Interface.class), is(empty()));
    }

    @Test
    public void immediate_interfaces_implemented_by_a_class() {
        analyze(InterfaceImplementer.class);

        assertThat(getInterfacesOf(InterfaceImplementer.class), is(classList(Interface.class)));
    }

    private interface Interface {
    }

    private class InterfaceImplementer implements Interface {
    }


    // API wrappers

    private void analyze(Class<?> clazz) {
        byte[] bytecode = readBytecode(clazz);
        analyzer.analyze(bytecode);
    }

    private List<Class<?>> getInterfaces() {
        return toClassList(analyzer.getInterfaces());
    }

    private List<Class<?>> getClasses() {
        return toClassList(analyzer.getClasses());
    }

    private List<Class<?>> getInterfacesOf(Class<?> clazz) {
        return toClassList(analyzer.getInterfacesOf(Type.getType(clazz)));
    }


    // other helpers

    private static List<Class<?>> toClassList(List<Type> types) {
        return types.stream()
                .map(ClassHierarchyAnalyzerTest::toClass)
                .collect(toList());
    }

    private static List<Class<?>> classList(Class<?>... aClass) {
        return asList(aClass);
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
