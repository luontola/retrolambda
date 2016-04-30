// Copyright © 2013-2016 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.google.common.base.MoreObjects;
import net.orfjackal.retrolambda.lambdas.Handles;
import org.objectweb.asm.*;

import java.util.Objects;

public final class MethodRef {

    // TODO: replace MethodRef with ASM's Handle, or merge with MethodInfo?

    public final int tag;
    public final String owner;
    public final String name;
    public final String desc;

    public MethodRef(int tag, Class<?> owner, String name, String desc) {
        this(tag, Type.getInternalName(owner), name, desc);
    }

    public MethodRef(int tag, String owner, String name, String desc) {
        this.tag = tag;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public MethodSignature getSignature() {
        return new MethodSignature(name, desc);
    }

    public int getOpcode() {
        return Handles.getOpcode(toHandle());
    }

    public Handle toHandle() {
        return new Handle(tag, owner, name, desc, tag == Opcodes.H_INVOKEINTERFACE);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodRef)) {
            return false;
        }
        // NOTE: the tag does not not affect method equality, because e.g. super calls have different tag but same method
        MethodRef that = (MethodRef) obj;
        return this.owner.equals(that.owner)
                && this.name.equals(that.name)
                && this.desc.equals(that.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, desc);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(owner)
                .addValue(name)
                .addValue(desc)
                .addValue("(" + tag + ")")
                .toString();
    }
}
