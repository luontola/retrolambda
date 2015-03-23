// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.*;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.*;

public class ClassHierarchyAnalyzer implements MethodRelocations {

    private static final MethodRef ABSTRACT_METHOD = new MethodRef("", "", "");

    private final Map<Type, ClassInfo> classes = new HashMap<>();
    @Deprecated
    private final Map<Type, Type> superclasses = new HashMap<>();
    @Deprecated
    private final Map<Type, List<MethodRef>> methodsByInterface = new HashMap<>();
    @Deprecated
    private final Map<Type, List<MethodRef>> methodsByClass = new HashMap<>();
    private final Map<MethodRef, MethodRef> relocatedMethods = new HashMap<>();
    private final Map<MethodRef, MethodRef> methodDefaultImpls = new HashMap<>();
    private final Map<String, String> companionClasses = new HashMap<>();

    public void analyze(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);

        ClassInfo c = new ClassInfo(cr);
        classes.put(c.type, c);

        superclasses.put(c.type, classNameToType(cr.getSuperName()));

        if (Flags.hasFlag(cr.getAccess(), ACC_INTERFACE)) {
            analyzeInterface(cr);
        } else {
            analyzeClass(cr);
        }
    }

    private void analyzeClass(ClassReader cr) {
        cr.accept(new ClassVisitor(ASM5) {
            private String owner;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.owner = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodRef method = new MethodRef(owner, name, desc);
                methodsByClass.computeIfAbsent(classNameToType(method.owner), key -> new ArrayList<>()).add(method);

                return null;
            }

        }, ClassReader.SKIP_CODE);
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
                    desc = Bytecode.prependArgumentType(desc, Type.getObjectType(owner));
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
                methodsByInterface.computeIfAbsent(classNameToType(method.owner), key -> new ArrayList<>()).add(method);
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

    public List<ClassInfo> getInterfaces() {
        return classes.values()
                .stream()
                .filter(ClassInfo::isInterface)
                .collect(toList());
    }

    public List<ClassInfo> getClasses() {
        return classes.values()
                .stream()
                .filter(ClassInfo::isClass)
                .collect(toList());
    }

    public List<Type> getInterfacesOf(Type type) {
        ClassInfo c = classes.get(type);
        if (c == null) {
            // non-analyzed class, probably from a class library
            return Collections.emptyList();
        }
        return c.interfaces;
    }

    @Override
    public MethodRef getMethodCallTarget(MethodRef original) {
        return relocatedMethods.getOrDefault(original, original);
    }

    @Override
    public MethodRef getMethodDefaultImplementation(MethodRef interfaceMethod) {
        MethodRef impl;
        List<Type> currentInterfaces = new ArrayList<>();
        List<Type> parentInterfaces = new ArrayList<>();
        currentInterfaces.add(Type.getObjectType(interfaceMethod.owner));

        do {
            for (Type anInterface : currentInterfaces) {
                impl = methodDefaultImpls.get(interfaceMethod.withOwner(anInterface.getInternalName()));
                if (impl == ABSTRACT_METHOD) {
                    return null;
                }
                if (impl != null) {
                    return impl;
                }
                parentInterfaces.addAll(getInterfacesOf(anInterface));
            }
            currentInterfaces = parentInterfaces;
            parentInterfaces = new ArrayList<>();
        } while (!currentInterfaces.isEmpty());

        return null;
    }

    @Override
    public List<MethodRef> getInterfaceMethods(Type interfaceName) {
        Set<MethodRef> results = new LinkedHashSet<>();
        results.addAll(methodsByInterface.getOrDefault(interfaceName, Collections.emptyList()));
        for (Type parent : getInterfacesOf(interfaceName)) {
            for (MethodRef parentMethod : getInterfaceMethods(parent)) {
                results.add(parentMethod.withOwner(interfaceName.getInternalName()));
            }
        }
        return new ArrayList<>(results);
    }

    public List<MethodRef> getSuperclassMethods(Type className) {
        Set<MethodRef> results = new LinkedHashSet<>();
        while (superclasses.containsKey(className)) {
            className = superclasses.get(className);
            results.addAll(methodsByClass.getOrDefault(className, Collections.emptyList()));
        }
        return new ArrayList<>(results);
    }

    @Override
    public String getCompanionClass(String className) {
        return companionClasses.get(className);
    }

    static List<Type> classNamesToTypes(String[] interfaces) {
        return Stream.of(interfaces)
                .map(ClassHierarchyAnalyzer::classNameToType)
                .collect(toList());
    }

    static Type classNameToType(String className) {
        return Type.getType("L" + className + ";");
    }
}
