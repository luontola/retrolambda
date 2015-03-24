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
    private final Map<MethodRef, MethodRef> relocatedMethods = new HashMap<>();
    private final Map<MethodRef, MethodRef> methodDefaultImpls = new HashMap<>();

    public void analyze(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);

        ClassInfo c = new ClassInfo(cr);
        classes.put(c.type, c);

        if (Flags.hasFlag(cr.getAccess(), ACC_INTERFACE)) {
            analyzeInterface(c, cr);
        } else {
            analyzeClass(c, cr);
        }
    }

    private void analyzeClass(ClassInfo c, ClassReader cr) {
        cr.accept(new ClassVisitor(ASM5) {
            private String owner;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.owner = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodRef method = new MethodRef(owner, name, desc);
                // FIXME: skip static methods
                c.addMethod(method);

                // XXX: backporting Retrolambda fails if we remove this; it tries backporting a lambda while backporting a lambda
                Runnable r = () -> {
                };
                return null;
            }

        }, ClassReader.SKIP_CODE);
    }

    private void analyzeInterface(ClassInfo c, ClassReader cr) {
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
                    c.addMethod(method);

                } else if (isDefaultMethod(access)) {
                    desc = Bytecode.prependArgumentType(desc, Type.getObjectType(owner));
                    methodDefaultImpls.put(method, new MethodRef(companion, name, desc));
                    c.enableCompanionClass();
                    c.addMethod(method);

                } else if (isStaticMethod(access)) {
                    relocatedMethods.put(method, new MethodRef(companion, name, desc));
                    c.enableCompanionClass();
                }
                return null;
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

    private ClassInfo getClass(Type type) {
        return classes.getOrDefault(type, new ClassInfo());
    }

    public List<Type> getInterfacesOf(Type type) {
        return getClass(type).interfaces;
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
    public List<MethodRef> getInterfaceMethods(Type type) {
        Set<MethodRef> results = new LinkedHashSet<>();
        results.addAll(getClass(type).methods);
        for (Type parent : getInterfacesOf(type)) {
            for (MethodRef parentMethod : getInterfaceMethods(parent)) {
                results.add(parentMethod.withOwner(type.getInternalName()));
            }
        }
        return new ArrayList<>(results);
    }

    @Override
    public List<MethodSignature> getSuperclassMethods(Type type) {
        Set<MethodRef> results = new LinkedHashSet<>();
        while (classes.containsKey(type)) {
            ClassInfo c = classes.get(type);
            type = c.superclass;
            results.addAll(getClass(type).methods);
        }
        return results.stream()
                .map(MethodRef::getSignature)
                .collect(toList());
    }

    @Override
    public Optional<Type> getCompanionClass(Type type) {
        return getClass(type).getCompanionClass();
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
