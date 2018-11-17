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
            Log.error("Failed to transform " + className, e);
            return null;
        }
    }

    private byte[] transformMetafactory(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("spinInnerClass")) {
                    mv = new MethodVisitor(Opcodes.ASM7, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                            if (name.equals("toByteArray")) {
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
        return cw.toByteArray();
    }
}
