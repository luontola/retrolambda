// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.google.common.base.Objects;
import org.objectweb.asm.Type;

public final class MethodRef {

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
        return Objects.hashCode(owner, name, desc);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(owner)
                .addValue(name)
                .addValue(desc)
                .toString();
    }
}
