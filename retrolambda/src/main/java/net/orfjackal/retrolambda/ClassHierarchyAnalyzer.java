// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.*;

public class ClassHierarchyAnalyzer implements MethodRelocations {

    private final List<ClassReader> interfaces = new ArrayList<>();
    private final List<ClassReader> classes = new ArrayList<>();
    private final Map<Type, List<Type>> interfacesByImplementer = new HashMap<>();
    private final Map<MethodRef, MethodRef> relocatedMethods = new HashMap<>();

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

        if (Flags.hasFlag(cr.getAccess(), ACC_INTERFACE)) {
            discoverRelocatedMethods(cr);
        }
    }

    private void discoverRelocatedMethods(ClassReader cr) {
        cr.accept(new ClassVisitor(ASM5) {
            private String owner;
            private String companion;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.owner = name;
                this.companion = name + "$";
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (Flags.hasFlag(access, ACC_STATIC)) {
                    relocatedMethods.put(
                            new MethodRef(owner, name, desc),
                            new MethodRef(companion, name, desc));
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
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

    @Override
    public MethodRef getMethodLocation(MethodRef original) {
        return relocatedMethods.getOrDefault(original, original);
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
