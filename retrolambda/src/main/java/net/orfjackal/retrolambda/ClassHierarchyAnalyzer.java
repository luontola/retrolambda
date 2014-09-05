// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class ClassHierarchyAnalyzer {

    private final List<ClassReader> interfaces = new ArrayList<>();
    private final List<ClassReader> classes = new ArrayList<>();
    private final Map<Type, List<Type>> interfacesByImplementer = new HashMap<>();

    public void analyze(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        Type clazz = classNameToType(cr.getClassName());

        if (Flags.hasFlag(cr.getAccess(), ACC_INTERFACE)) {
            interfaces.add(cr);
        } else {
            classes.add(cr);
        }

        List<Type> interfaces = classNamesToTypes(cr.getInterfaces());
        interfacesByImplementer.put(clazz, interfaces);
    }

    public List<ClassReader> getInterfaces() {
        return interfaces;
    }

    public List<ClassReader> getClasses() {
        return classes;
    }

    public List<Type> getInterfacesOf(Type type) {
        return interfacesByImplementer.get(type);
    }

    private static List<Type> classNamesToTypes(String[] interfaces) {
        return Stream.of(interfaces)
                .map(ClassHierarchyAnalyzer::classNameToType)
                .collect(toList());
    }

    private static Type classNameToType(String className) {
        return Type.getType("L" + className + ";");
    }
}
