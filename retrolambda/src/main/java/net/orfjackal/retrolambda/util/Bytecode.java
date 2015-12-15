// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.util;

import net.orfjackal.retrolambda.lambdas.Handles;
import org.objectweb.asm.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class Bytecode {

    private static final Map<Integer, String> bytecodeVersionNames = new HashMap<>();

    static {
        bytecodeVersionNames.put(Opcodes.V1_1, "Java 1.1");
        bytecodeVersionNames.put(Opcodes.V1_2, "Java 1.2");
        bytecodeVersionNames.put(Opcodes.V1_3, "Java 1.3");
        bytecodeVersionNames.put(Opcodes.V1_4, "Java 1.4");
        bytecodeVersionNames.put(Opcodes.V1_5, "Java 5");
        bytecodeVersionNames.put(Opcodes.V1_6, "Java 6");
        bytecodeVersionNames.put(Opcodes.V1_7, "Java 7");
        bytecodeVersionNames.put(Opcodes.V1_8, "Java 8");
    }

    public static String getJavaVersion(int bytecodeVersion) {
        return bytecodeVersionNames.getOrDefault(bytecodeVersion, "unknown version");
    }

    public static void generateDelegateMethod(ClassVisitor cv, int access, Handle method, Handle target) {
        MethodVisitor mv = cv.visitMethod(access, method.getName(), method.getDesc(), null, null);
        mv.visitCode();

        // if the target method is constructor, then we must NEW up the instance inside the delegate method
        if (target.getTag() == H_NEWINVOKESPECIAL) {
            mv.visitTypeInsn(NEW, target.getOwner());
            mv.visitInsn(DUP);
        }

        // we assume one of the methods to be static and the other virtual, i.e. it has an implicit 'this' argument
        Type[] args = longest(
                Type.getArgumentTypes(method.getDesc()),
                Type.getArgumentTypes(target.getDesc()));
        int varIndex = 0;
        for (Type arg : args) {
            mv.visitVarInsn(arg.getOpcode(ILOAD), varIndex);
            varIndex += arg.getSize();
        }
        mv.visitMethodInsn(Handles.getOpcode(target), target.getOwner(), target.getName(), target.getDesc(), target.getTag() == H_INVOKEINTERFACE);
        mv.visitInsn(Type.getReturnType(method.getDesc()).getOpcode(IRETURN));
        mv.visitMaxs(-1, -1); // rely on ClassWriter.COMPUTE_MAXS
        mv.visitEnd();
    }

    private static Type[] longest(Type[] t1, Type[] t2) {
        return t1.length > t2.length ? t1 : t2;
    }

    public static String prependArgumentType(String desc, Type type) {
        Type returnType = Type.getReturnType(desc);
        Type[] args = Type.getArgumentTypes(desc);

        Type[] newArgs = new Type[args.length + 1];
        newArgs[0] = type;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return Type.getMethodDescriptor(returnType, newArgs);
    }
}
