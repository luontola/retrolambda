// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public class MethodInfo {

    public final MethodSignature signature;
    public final MethodKind kind;

    public MethodInfo(String name, String desc, MethodKind kind) {
        this(new MethodSignature(name, desc), kind);
    }

    public MethodInfo(MethodSignature signature, MethodKind kind) {
        this.signature = signature;
        this.kind = kind;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodInfo)) {
            return false;
        }
        MethodInfo that = (MethodInfo) obj;
        return this.signature.equals(that.signature)
                && this.kind.equals(that.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature, kind);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(signature)
                .addValue(kind)
                .toString();
    }
}
