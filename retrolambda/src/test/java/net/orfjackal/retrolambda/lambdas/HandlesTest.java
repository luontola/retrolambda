// Copyright © 2013-2016 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.junit.Test;
import org.objectweb.asm.*;

import static net.orfjackal.retrolambda.lambdas.Handles.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.objectweb.asm.Opcodes.*;

public class HandlesTest {

    @Test
    public void testGetOpcode() {
        assertThat(getOpcode(handle(H_INVOKEVIRTUAL)), is(INVOKEVIRTUAL));
        assertThat(getOpcode(handle(H_INVOKESTATIC)), is(INVOKESTATIC));
        assertThat(getOpcode(handle(H_INVOKESPECIAL)), is(INVOKESPECIAL));
        assertThat(getOpcode(handle(H_NEWINVOKESPECIAL)), is(INVOKESPECIAL));
        assertThat(getOpcode(handle(H_INVOKEINTERFACE)), is(INVOKEINTERFACE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOpcodeNegative() {
        getOpcode(handle(0));
    }

    @Test
    public void testOpcodeToTag() {
        assertThat(opcodeToTag(INVOKEVIRTUAL), is(H_INVOKEVIRTUAL));
        assertThat(opcodeToTag(INVOKESTATIC), is(H_INVOKESTATIC));
        assertThat(opcodeToTag(INVOKESPECIAL), is(H_INVOKESPECIAL));
        assertThat(opcodeToTag(INVOKEINTERFACE), is(H_INVOKEINTERFACE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOpcodeToTagNegative() {
        opcodeToTag(0);
    }

    @Test
    public void testAccessToTag() {
        assertThat(accessToTag(ACC_STATIC, true), is(H_INVOKESTATIC));
        assertThat(accessToTag(ACC_STATIC, false), is(H_INVOKESTATIC));

        assertThat(accessToTag(ACC_PRIVATE, true), is(H_INVOKESPECIAL));
        assertThat(accessToTag(ACC_PRIVATE, false), is(H_INVOKESPECIAL));

        assertThat(accessToTag(ACC_PUBLIC, true), is(H_INVOKEINTERFACE));
        assertThat(accessToTag(ACC_PUBLIC, false), is(H_INVOKEVIRTUAL));
    }

    private Handle handle(int tag) {
        return new Handle(tag, null, null, null, tag == Opcodes.H_INVOKEINTERFACE);
    }
}
