// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.*;
import org.objectweb.asm.*;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.*;

public class ClassHierarchyAnalyzer {

    private final Map<Type, ClassInfo> classes = new HashMap<>();
    private final Map<MethodRef, MethodRef> relocatedMethods = new HashMap<>();

    public void analyze(byte[] bytecode) {
        analyze(new ClassReader(bytecode));
    }

    public void analyze(ClassReader cr) {
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
                if (isConstructor(name) || isStaticMethod(access)) {
                    return null;
                }
                c.addMethod(new MethodRef(owner, name, desc), new MethodKind.Implemented());
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
                    c.addMethod(method, new MethodKind.Abstract());

                } else if (isDefaultMethod(access)) {
                    MethodRef defaultImpl = new MethodRef(companion, name, Bytecode.prependArgumentType(desc, Type.getObjectType(owner)));
                    c.enableCompanionClass();
                    c.addMethod(method, new MethodKind.Default(defaultImpl));

                } else if (isStaticMethod(access)) {
                    relocatedMethods.put(method, new MethodRef(companion, name, desc));
                    c.enableCompanionClass();
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
    }

    private static boolean isConstructor(String name) {
        return name.equals("<init>");
    }

    private static boolean isAbstractMethod(int access) {
        return Flags.hasFlag(access, ACC_ABSTRACT);
    }

    private static boolean isStaticMethod(int access) {
        return Flags.hasFlag(access, ACC_STATIC);
    }

    private static boolean isDefaultMethod(int access) {
        return !isAbstractMethod(access)
                && !isStaticMethod(access);
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

    public MethodRef getMethodCallTarget(MethodRef original) {
        return relocatedMethods.getOrDefault(original, original);
    }

    public MethodRef getMethodDefaultImplementation(MethodRef interfaceMethod) {
        MethodSignature signature = interfaceMethod.getSignature();
        for (MethodInfo method : getDefaultMethods(Type.getObjectType(interfaceMethod.owner))) {
            if (method.signature.equals(signature)) {
                return method.getDefaultMethodImpl();
            }
        }
        return null;
    }

    public Optional<Type> getCompanionClass(Type type) {
        return getClass(type).getCompanionClass();
    }

    public List<MethodInfo> getDefaultMethods(Type type) {
        return getMethods(type).stream()
                .filter(m -> m.kind instanceof MethodKind.Default)
                .collect(toList());
    }

    public Collection<MethodInfo> getMethods(Type type) {
        ClassInfo c = getClass(type);
        Map<MethodSignature, MethodInfo> methods = new HashMap<>();

        // in reverse priority order:
        // - default methods
        for (Type iface : c.getInterfaces()) {
            for (MethodInfo m : getMethods(iface)) {
                if (!isAlreadyInherited(m, methods)) {
                    methods.put(m.signature, m);
                }
            }
        }
        // - superclass methods
        if (c.superclass != null) {
            for (MethodInfo m : getMethods(c.superclass)) {
                if (!isAlreadyInherited(m, methods)) {
                    methods.put(m.signature, m);
                }
            }
        }
        // - own methods
        for (MethodInfo m : c.getMethods()) {
            methods.put(m.signature, m);
        }
        return methods.values();
    }

    private boolean isAlreadyInherited(MethodInfo subject, Map<MethodSignature, MethodInfo> existingMethods) {
        MethodInfo existing = existingMethods.get(subject.signature);
        return existing != null && getAllInterfaces(existing.owner).contains(subject.owner);
    }

    private Set<Type> getAllInterfaces(Type interfaceType) {
        assert getClass(interfaceType).isInterface() : "not interface: " + interfaceType;
        HashSet<Type> results = new HashSet<>();
        results.add(interfaceType);
        for (Type parentInterface : getClass(interfaceType).getInterfaces()) {
            results.addAll(getAllInterfaces(parentInterface));
        }
        return results;
    }
}
