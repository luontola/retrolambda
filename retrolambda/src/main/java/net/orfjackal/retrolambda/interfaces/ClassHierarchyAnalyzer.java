// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.Flags;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.*;

public class ClassHierarchyAnalyzer implements MethodRelocations {

    private static final MethodRef ABSTRACT_METHOD = new MethodRef("", "", "");

    private final List<ClassReader> interfaces = new ArrayList<>();
    private final List<ClassReader> classes = new ArrayList<>();
    private final Map<Type, List<Type>> interfacesByImplementer = new HashMap<>(); // TODO: could use just String instead of Type
    private final Map<String, List<MethodRef>> methodsByInterface = new HashMap<>();
    private final Map<MethodRef, MethodRef> relocatedMethods = new HashMap<>();
    private final Map<MethodRef, MethodRef> methodDefaultImpls = new HashMap<>();
    private final Map<String, String> companionClasses = new HashMap<>();

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
            analyzeInterface(cr);
        }
    }

    private void analyzeInterface(ClassReader cr) {
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
                MethodRef method = new MethodRef(owner, name, desc);

                if (isAbstractMethod(access)) {
                    methodDefaultImpls.put(method, ABSTRACT_METHOD);
                    saveInterfaceMethod(method);

                } else if (isDefaultMethod(access)) {
                    methodDefaultImpls.put(method, new MethodRef(companion, name, desc));
                    companionClasses.put(owner, companion);
                    saveInterfaceMethod(method);

                } else if (isStaticMethod(access)) {
                    relocatedMethods.put(method, new MethodRef(companion, name, desc));
                    companionClasses.put(owner, companion);
                }
                return null;
            }

            private void saveInterfaceMethod(MethodRef method) {
                methodsByInterface.computeIfAbsent(method.owner, key -> new ArrayList<>()).add(method);
            }

            private boolean isAbstractMethod(int access) {
                return Flags.hasFlag(access, ACC_ABSTRACT);
            }

            private boolean isStaticMethod(int access) {
                return Flags.hasFlag(access, ACC_STATIC);
            }

            private boolean isDefaultMethod(int access) {
                return !isAbstractMethod(access)
                        && !isStaticMethod(access);
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
    public MethodRef getMethodCallTarget(MethodRef original) {
        return relocatedMethods.getOrDefault(original, original);
    }

    @Override
    public MethodRef getMethodDefaultImplementation(MethodRef interfaceMethod) {
        MethodRef impl = methodDefaultImpls.get(interfaceMethod);
        if (impl == ABSTRACT_METHOD) {
            return null;
        }
        if (impl != null) {
            return impl;
        }

        // check if a default implementation is inherited from parents
        for (Type parentInterface : interfacesByImplementer.getOrDefault(Type.getObjectType(interfaceMethod.owner), Collections.emptyList())) {
            impl = getMethodDefaultImplementation(interfaceMethod.withOwner(parentInterface.getInternalName()));
            if (impl != null) {
                return impl;
            }
        }
        return null;
    }

    @Override
    public List<MethodRef> getInterfaceMethods(String interfaceName) {
        return methodsByInterface.getOrDefault(interfaceName, Collections.emptyList());
    }

    @Override
    public String getCompanionClass(String className) {
        return companionClasses.get(className);
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
