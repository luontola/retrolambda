// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.google.common.base.MoreObjects;
import org.objectweb.asm.Type;

import java.util.Objects;

public class MethodInfo {

    public final MethodSignature signature;
    public final Type owner;
    public final MethodKind kind;

    public MethodInfo(String name, String desc, Class<?> owner, MethodKind kind) {
        this(new MethodSignature(name, desc), Type.getType(owner), kind);
    }

    public MethodInfo(MethodSignature signature, Type owner, MethodKind kind) {
        this.signature = signature;
        this.owner = owner;
        this.kind = kind;
    }

    public MethodRef toMethodRef() {
        return new MethodRef(owner.getInternalName(), signature.name, signature.desc);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodInfo)) {
            return false;
        }
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
                .toString();
    }
}
