// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.Flags;
import org.objectweb.asm.*;

import java.util.*;

import static net.orfjackal.retrolambda.interfaces.ClassHierarchyAnalyzer.*;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class ClassInfo {

    public final ClassReader reader;
    private final int access;
    public final Type type;
    public final Type superclass;
    public final List<Type> interfaces;
    public final List<MethodRef> methods = new ArrayList<>();
    private Type companion;

    public ClassInfo() {
        this.reader = null;
        this.access = 0;
        this.type = null;
        this.superclass = null;
        this.interfaces = new ArrayList<>();
    }

    public ClassInfo(ClassReader cr) {
        this.reader = cr;
        this.access = cr.getAccess();
        this.type = classNameToType(cr.getClassName());
        this.superclass = classNameToType(cr.getSuperName());
        this.interfaces = classNamesToTypes(cr.getInterfaces());
    }

    public void addMethod(MethodRef method) {
        methods.add(method);
    }

    public void enableCompanionClass() {
        this.companion = Type.getObjectType(type.getInternalName() + "$");
    }

    public Optional<Type> getCompanionClass() {
        return Optional.ofNullable(companion);
    }

    public boolean isClass() {
        return !isInterface();
    }

    public boolean isInterface() {
        return Flags.hasFlag(access, ACC_INTERFACE);
    }
}
