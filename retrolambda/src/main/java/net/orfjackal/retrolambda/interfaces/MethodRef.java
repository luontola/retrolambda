// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.google.common.base.MoreObjects;
import org.objectweb.asm.*;

import java.util.Objects;

public final class MethodRef {

    // TODO: replace MethodRef with ASM's Handle, or merge with MethodInfo?

    public final String owner;
    public final String name;
    public final String desc;

    public MethodRef(Class<?> owner, String name, String desc) {
        this(Type.getInternalName(owner), name, desc);
    }

    public MethodRef(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public MethodSignature getSignature() {
        return new MethodSignature(name, desc);
    }

    public Handle toHandle(int tag) {
        return new Handle(tag, owner, name, desc);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodRef)) {
            return false;
        }
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
                .toString();
    }
}
