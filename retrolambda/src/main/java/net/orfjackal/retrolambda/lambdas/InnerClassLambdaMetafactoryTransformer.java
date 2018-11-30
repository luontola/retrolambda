// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import com.esotericsoftware.minlog.Log;
import net.orfjackal.retrolambda.Agent;
import org.objectweb.asm.*;

import java.lang.instrument.*;
import java.security.ProtectionDomain;

public class InnerClassLambdaMetafactoryTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
        if (!"java/lang/invoke/InnerClassLambdaMetafactory".equals(className)) {
            return null;
        }

        try {
            byte[] transformed = transformMetafactory(bytes);
            Agent.enable();
            return transformed;
        } catch (Throwable e) {
            Log.error("Failed to transform " + className + ", cannot enable the Java agent. " +
                    "Please report an issue to Retrolambda with full logs. " +
                    "Probably you're running on an unsupported Java version.", e);
            return null;
        }
    }

    private byte[] transformMetafactory(byte[] bytes) {
        final boolean[] spinInnerClassFound = {false};
        final boolean[] toByteArrayFound = {false};
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("spinInnerClass")) {
                    spinInnerClassFound[0] = true;
                    mv = new MethodVisitor(Opcodes.ASM7, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                            if (name.equals("toByteArray")) {
                                if (toByteArrayFound[0]) {
                                    throw new RuntimeException("Found multiple toByteArray calls");
                                } else {
                                    toByteArrayFound[0] = true;
                                }
                                mv.visitInsn(Opcodes.DUP);
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Agent.class), "saveLambda", "([B)V", false);
                            }
                        }

                        @Override
                        public void visitMaxs(int maxStack, int maxLocals) {
                            super.visitMaxs(maxStack + 1, maxLocals);
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, 0);
        if (!spinInnerClassFound[0]) {
            throw new RuntimeException("Could not find the spinInnerClass method");
        }
        if (!toByteArrayFound[0]) {
            throw new RuntimeException("Could not find the toByteArray call");
        }
        return cw.toByteArray();
    }
}
