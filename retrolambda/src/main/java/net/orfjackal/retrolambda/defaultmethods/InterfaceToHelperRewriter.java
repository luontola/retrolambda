// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.defaultmethods;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
* Created by arneball on 2014-08-24.
*/
class InterfaceToHelperRewriter extends MethodVisitor implements Opcodes {
	public InterfaceToHelperRewriter(MethodVisitor mv) {
		super(ASM5, mv);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if(opcode == INVOKESPECIAL && itf){
			super.visitMethodInsn(INVOKESTATIC, owner + "$helper", name, Helpers.addParam(desc, owner), false);
		} else if(opcode == INVOKESTATIC && itf) {
			super.visitMethodInsn(INVOKESTATIC, owner + "$helper", name + "$static", desc, false);
		} else {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}
}
