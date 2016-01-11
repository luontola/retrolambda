// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.google.common.base.MoreObjects;
import org.objectweb.asm.Type;

import java.util.Objects;

public class MethodInfo {

    public final int access;
    public final int tag;
    public final MethodSignature signature;
    public final Type owner;
    public final MethodKind kind;

    public MethodInfo(String name, String desc, Class<?> owner, MethodKind kind) {
        // only for tests, so we can ignore the tag and access
        this(0, -1, new MethodSignature(name, desc), Type.getType(owner), kind);
    }

    public MethodInfo(int access, int tag, MethodSignature signature, Type owner, MethodKind kind) {
        this.access = access;
        this.tag = tag;
        this.signature = signature;
        this.owner = owner;
        this.kind = kind;
    }

    public MethodRef getDefaultMethodImpl() {
        return ((MethodKind.Default) kind).defaultImpl;
    }

    public MethodRef toMethodRef() {
        return new MethodRef(tag, owner.getInternalName(), signature.name, signature.desc);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodInfo)) {
            return false;
        }
        // NOTE: the tag does not not affect method equality, because e.g. super calls have different tag but same method
        MethodInfo that = (MethodInfo) obj;
        return this.signature.equals(that.signature)
                && this.owner.equals(that.owner)
                && this.kind.equals(that.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature, owner, kind);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(signature)
                .addValue(owner)
                .addValue(kind)
                .addValue("(tag=" + tag + ", access=" + access + ")")
                .toString();
    }
}
