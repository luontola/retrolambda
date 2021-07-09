// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.*;

import java.lang.invoke.*;

import static org.objectweb.asm.Opcodes.*;

public class Types {

    public static Object asmToJdkType(Object arg, ClassLoader classLoader, MethodHandles.Lookup caller) throws Exception {
        if (arg instanceof Type) {
            Type type = (Type) arg;
            if (type.getSort() == Type.METHOD) {
                return toMethodType(type, classLoader);
            } else if (type.getSort() == Type.OBJECT) {
                return toClass(type, classLoader);
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type);
            }
        } else if (arg instanceof Handle) {
            return toMethodHandle((Handle) arg, classLoader, caller);
        } else {
            return arg;
        }
    }

    public static MethodType toMethodType(Type type, ClassLoader classLoader) {
        return MethodType.fromMethodDescriptorString(type.getInternalName(), classLoader);
    }

    private static Class<?> toClass(Type type, ClassLoader classLoader) throws ClassNotFoundException {
        return classLoader.loadClass(type.getInternalName().replace('/', '.'));
    }

    public static MethodHandle toMethodHandle(Handle handle, ClassLoader classLoader, MethodHandles.Lookup lookup) throws Exception {
        MethodType type = MethodType.fromMethodDescriptorString(handle.getDesc(), classLoader);
        final String className = handle.getOwner().replace('/', '.');
        Class<?> owner = Class.forName(className, false, classLoader);
        switch (handle.getTag()) {
            case H_INVOKESTATIC:
                return lookup.findStatic(owner, handle.getName(), type);

            case H_INVOKEVIRTUAL:
            case H_INVOKEINTERFACE:
                return lookup.findVirtual(owner, handle.getName(), type);

            case H_INVOKESPECIAL:
                return lookup.findSpecial(owner, handle.getName(), type, owner);

            case H_NEWINVOKESPECIAL:
                return lookup.findConstructor(owner, type);

            default:
                throw new AssertionError("Unexpected handle type: " + handle);
        }
    }

    public static String changeReturnType(Type returnType, String methodDescriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
        return Type.getMethodDescriptor(returnType, argumentTypes);
    }

    public static String prependArgumentType(Type argumentType, String methodDescriptor) {
        Type returnType = Type.getReturnType(methodDescriptor);
        Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
        return Type.getMethodDescriptor(returnType, conj(argumentType, argumentTypes));
    }

    private static Type[] conj(Type type, Type[] types) {
        Type[] result = new Type[types.length + 1];
        result[0] = type;
        System.arraycopy(types, 0, result, 1, types.length);
        return result;
    }
}
