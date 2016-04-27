// Copyright 2016 The Retrolambda Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.orfjackal.retrolambda.requirenonnull;

import org.objectweb.asm.*;

/**
 * Rewrites calls to {@code Objects.requireNonNull}, which is only available in JDK 7 and above.
 *
 * <p>Starting in JDK 9, javac uses {@code requireNonNull} for synthetic null-checks
 * (see <a href="http://bugs.openjdk.java.net/browse/JDK-8074306">JDK-8074306</a>).
 */
public class RequireNonNull extends ClassVisitor {

    public RequireNonNull(ClassVisitor next) {
        super(Opcodes.ASM5, next);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM5, next) {
            @Override
            public void visitMethodInsn(
                    int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKESTATIC
                        && owner.equals("java/util/Objects")
                        && name.equals("requireNonNull")
                        && desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;")) {
                    super.visitInsn(Opcodes.DUP);
                    super.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/Object",
                            "getClass",
                            "()Ljava/lang/Class;",
                            false);
                    super.visitInsn(Opcodes.POP);
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        };
    }
}
