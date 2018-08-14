// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.ext.ow2asm.EnhancedClassReader;
import net.orfjackal.retrolambda.interfaces.*;
import net.orfjackal.retrolambda.lambdas.*;
import net.orfjackal.retrolambda.util.*;
import org.objectweb.asm.*;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static net.orfjackal.retrolambda.util.Flags.*;
import static org.objectweb.asm.Opcodes.*;

public class ClassAnalyzer {

    private final Map<Type, ClassInfo> classes = new HashMap<>();
    private final Map<MethodRef, MethodRef> relocatedMethods = new HashMap<>();
    private final Map<MethodRef, MethodRef> renamedLambdaMethods = new HashMap<>();

    public void analyze(byte[] bytecode, boolean isJavacHacksEnabled) {
        analyze(EnhancedClassReader.create(bytecode, isJavacHacksEnabled));
    }

    public void analyze(ClassReader cr) {
        ClassInfo c = new ClassInfo(cr);
        classes.put(c.type, c);

        if (isInterface(cr.getAccess())) {
            analyzeInterface(c, cr);
        } else {
            analyzeClass(c, cr);
        }
        analyzeClassOrInterface(c, cr);
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
                int tag;
                if (isConstructor(name)) {
                    tag = H_INVOKESPECIAL;
                } else if (isStaticMethod(access)) {
                    tag = H_INVOKESTATIC;
                } else {
                    tag = H_INVOKEVIRTUAL;
                }

                c.addMethod(access, new MethodRef(tag, owner, name, desc), new MethodKind.Implemented());
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
                MethodRef method = new MethodRef(Handles.accessToTag(access, true), owner, name, desc);

                if (isAbstractMethod(access)) {
                    c.addMethod(access, method, new MethodKind.Abstract());

                } else if (isDefaultMethod(access)) {
                    MethodRef defaultImpl = new MethodRef(H_INVOKESTATIC, companion, name, Bytecode.prependArgumentType(desc, Type.getObjectType(owner)));
                    c.enableCompanionClass();
                    c.addMethod(access, method, new MethodKind.Default(defaultImpl));

                } else if (isInstanceLambdaImplMethod(access)) {
                    relocatedMethods.put(method, new MethodRef(H_INVOKESTATIC, companion, name, Bytecode.prependArgumentType(desc, Type.getObjectType(owner))));
                    c.enableCompanionClass();

                } else if (isStaticMethod(access) && !isStaticInitializer(name, desc, access)) {
                    relocatedMethods.put(method, new MethodRef(H_INVOKESTATIC, companion, name, desc));
                    c.enableCompanionClass();
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
    }

    private void analyzeClassOrInterface(ClassInfo c, ClassReader cr) {
        cr.accept(new ClassVisitor(ASM5) {
            private String owner;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.owner = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodRef method = new MethodRef(Handles.accessToTag(access, true), owner, name, desc);

                // XXX: duplicates code in net.orfjackal.retrolambda.lambdas.BackportLambdaInvocations.visitMethod()
                if (LambdaNaming.isBodyMethod(access, name)
                        && Flags.isPrivateMethod(access)
                        && Flags.isInstanceMethod(access)) {
                    desc = Types.prependArgumentType(Type.getObjectType(owner), desc); // add 'this' as first parameter
                    renamedLambdaMethods.put(method, new MethodRef(H_INVOKESTATIC, owner, name, desc));
                }

                return null;
            }
        }, ClassReader.SKIP_CODE);
    }

    private static boolean isDefaultMethod(int access) {
        return !isAbstractMethod(access)
                && !isStaticMethod(access)
                && isPublicMethod(access);
    }

    private static boolean isInstanceLambdaImplMethod(int access) {
        return !isAbstractMethod(access)
                && !isStaticMethod(access)
                && isPrivateMethod(access);
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
        if (original.tag == H_INVOKESPECIAL) {
            // change Interface.super.defaultMethod() calls to static calls on the companion class
            MethodRef impl = getMethodDefaultImplementation(original);
            if (impl != null) {
                return impl;
            }
        }
        return relocatedMethods.getOrDefault(original, original);
    }

    public MethodRef getRenamedLambdaMethod(MethodRef original) {
        return renamedLambdaMethods.getOrDefault(original, original);
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
