// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import com.esotericsoftware.minlog.Log;
import net.orfjackal.retrolambda.ClassAnalyzer;
import net.orfjackal.retrolambda.interfaces.*;
import net.orfjackal.retrolambda.util.*;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.orfjackal.retrolambda.util.Flags.isInterface;
import static org.objectweb.asm.Opcodes.*;

public class BackportLambdaInvocations extends ClassVisitor {

    private int classAccess;
    private String className;
    private final ClassAnalyzer analyzer;
    private final Map<Handle, Handle> lambdaAccessToImplMethods = new LinkedHashMap<>();
    private final EnclosingClass enclosingClass = new EnclosingClass();

    public BackportLambdaInvocations(ClassVisitor next, ClassAnalyzer analyzer) {
        super(ASM5, next);
        this.analyzer = analyzer;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        resetLambdaClassSequenceNumber();
        this.classAccess = access;
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        enclosingClass.sourceFile = source;
        super.visitSource(source, debug);
    }

    private static void resetLambdaClassSequenceNumber() {
        try {
            Field counterField = Class.forName("java.lang.invoke.InnerClassLambdaMetafactory").getDeclaredField("counter");
            counterField.setAccessible(true);
            AtomicInteger counter = (AtomicInteger) counterField.get(null);
            counter.set(0);
        } catch (Throwable t) {
            Log.warn("Failed to start class numbering from one. Don't worry, it's cosmetic, " +
                    "but please file a bug report and tell on which JDK version this happened.", t);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (LambdaNaming.isBodyMethod(access, name)) {

            // Ensure the generated lambda class is able to call this method.
            if (Flags.isPrivateMethod(access)) {
                access &= ~ACC_PRIVATE; // make non-private

                // Making private instance methods non-private is dangerous, because subclasses
                // may then override them. That's why we will also make them static, so that they
                // will not be overridable.
                if (Flags.isInstanceMethod(access)) {
                    access |= ACC_STATIC; // make static
                    desc = Types.prependArgumentType(Type.getObjectType(className), desc); // add 'this' as first parameter
                }
            }
        }
        if (LambdaNaming.isDeserializationHook(access, name, desc)) {
            return null; // remove serialization hooks; we serialize lambda instances as-is
        }
        return new InvokeDynamicInsnConverter(super.visitMethod(access, name, desc, signature, exceptions));
    }

    Handle getLambdaAccessMethod(Handle implMethod) {
        if (!implMethod.getOwner().equals(className)) {
            if (isNonOwnedMethodVisible(implMethod)) {
                return implMethod;
            }
        } else {
            if (isInterface(classAccess)) {
                // the method will be relocated to a companion class
                return implMethod;
            }
            if (isOwnedMethodVisible(implMethod)) {
                // The method is visible to the companion class and therefore doesn't need an accessor.
                return implMethod;
            }
            if (LambdaNaming.isBodyMethodName(implMethod.getName())) {
                if (implMethod.getTag() == H_INVOKESPECIAL) {
                    // The private body method was changed from a private instance method into
                    // a non-private static method, so change its invocation from special to static.
                    String desc = Types.prependArgumentType(Type.getObjectType(implMethod.getOwner()), implMethod.getDesc());
                    return new Handle(H_INVOKESTATIC, implMethod.getOwner(), implMethod.getName(), desc, false);
                }
                return implMethod;
            }
        }
        String name = "access$lambda$" + lambdaAccessToImplMethods.size();
        String desc = getLambdaAccessMethodDesc(implMethod);
        Handle accessMethod = new Handle(H_INVOKESTATIC, className, name, desc, false);
        lambdaAccessToImplMethods.put(accessMethod, implMethod);
        return accessMethod;
    }

    private boolean isOwnedMethodVisible(Handle implMethod) {
        MethodSignature implSignature = new MethodSignature(implMethod.getName(), implMethod.getDesc());

        Collection<MethodInfo> methods = analyzer.getMethods(Type.getObjectType(implMethod.getOwner()));
        for (MethodInfo method : methods) {
            if (method.signature.equals(implSignature)) {
                // The method will be visible to the companion class if the private flag is absent.
                return (method.access & ACC_PRIVATE) == 0;
            }
        }
        throw new IllegalStateException("Non-analyzed method " + implMethod + ". Report this as a bug.");
    }

    private boolean isNonOwnedMethodVisible(Handle implMethod) {
        if (getPackage(className).equals(getPackage(implMethod.getOwner()))) {
            return true; // All method visibilities in the same package will be visible.
        }

        MethodSignature implSignature = new MethodSignature(implMethod.getName(), implMethod.getDesc());

        Collection<MethodInfo> methods = analyzer.getMethods(Type.getObjectType(implMethod.getOwner()));
        for (MethodInfo method : methods) {
            if (method.signature.equals(implSignature)) {
                // The method will be visible to the companion class if the protected flag is absent.
                return (method.access & ACC_PROTECTED) == 0;
            }
        }
        return true;
    }

    private static String getPackage(String className) {
        int lastSlash = className.lastIndexOf('/');
        return lastSlash == -1 ? "" : className.substring(0, lastSlash);
    }

    private String getLambdaAccessMethodDesc(Handle implMethod) {
        if (implMethod.getTag() == H_INVOKESTATIC) {
            // static method call -> keep as-is
            return implMethod.getDesc();

        } else if (implMethod.getTag() == H_NEWINVOKESPECIAL) {
            // constructor call -> change to a a factory method
            return Types.changeReturnType(Type.getObjectType(implMethod.getOwner()), implMethod.getDesc());

        } else {
            // instance method call -> change to a static method
            return Types.prependArgumentType(Type.getObjectType(className), implMethod.getDesc());
        }
    }

    @Override
    public void visitEnd() {
        for (Map.Entry<Handle, Handle> entry : lambdaAccessToImplMethods.entrySet()) {
            Handle accessMethod = entry.getKey();
            Handle implMethod = entry.getValue();
            Bytecode.generateDelegateMethod(cv, ACC_STATIC | ACC_SYNTHETIC, accessMethod, implMethod);
        }
        super.visitEnd();
    }


    private class InvokeDynamicInsnConverter extends MethodVisitor {

        public InvokeDynamicInsnConverter(MethodVisitor next) {
            super(ASM5, next);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (bsm.getOwner().equals(LambdaNaming.LAMBDA_METAFACTORY)) {
                backportLambda(name, Type.getType(desc), bsm, bsmArgs);
            } else {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            }
        }

        private void backportLambda(String invokedName, Type invokedType, Handle bsm, Object[] bsmArgs) {
            Class<?> invoker = loadClass(className);
            Handle implMethod = (Handle) bsmArgs[1];
            Handle accessMethod = getLambdaAccessMethod(implMethod);

            LambdaFactoryMethod factory = LambdaReifier.reifyLambdaClass(enclosingClass, implMethod, accessMethod,
                    invoker, invokedName, invokedType, bsm, bsmArgs);
            super.visitMethodInsn(INVOKESTATIC, factory.getOwner(), factory.getName(), factory.getDesc(), false);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return cl.loadClass(className.replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
