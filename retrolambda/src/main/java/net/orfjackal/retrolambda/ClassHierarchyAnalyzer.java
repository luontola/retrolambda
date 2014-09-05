// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ClassHierarchyAnalyzer {

    private final Map<Type, List<Type>> interfacesByImplementer = new HashMap<>();

    public void analyze(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        Type implementer = classNameToType(cr.getClassName());
        List<Type> interfaces = classNamesToTypes(cr.getInterfaces());
        interfacesByImplementer.put(implementer, interfaces);
    }

    public List<Type> getInterfaces(Type type) {
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
