// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public class MethodSignature {

    public final String name;
    public final String desc;

    public MethodSignature(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodSignature)) {
            return false;
        }
        MethodSignature that = (MethodSignature) obj;
        return this.name.equals(that.name)
                && this.desc.equals(that.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(name)
                .addValue(desc)
                .toString();
    }
}
