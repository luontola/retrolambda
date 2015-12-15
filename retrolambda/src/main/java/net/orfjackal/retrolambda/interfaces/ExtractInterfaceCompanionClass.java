// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import net.orfjackal.retrolambda.util.Bytecode;
import org.objectweb.asm.*;

import static net.orfjackal.retrolambda.util.Flags.*;
import static org.objectweb.asm.Opcodes.*;

public class ExtractInterfaceCompanionClass extends ClassVisitor {

    private final Type companion;
    private String interfaceName;

    public ExtractInterfaceCompanionClass(ClassVisitor next, Type companion) {
        super(ASM5, next);
        this.companion = companion;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        interfaceName = name;
        name = companion.getInternalName();
        access &= ~ACC_INTERFACE;
        access &= ~ACC_ABSTRACT;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (isAbstractMethod(access)) {
            // do not copy abstract methods to the companion class
            return null;
        }
        if (isStaticInitializer(name, desc, access)) {
            // we won't copy constant fields from the interface, so a class initializer won't be needed
            return null;
        }
        if (isPrivateMethod(access)) {
            // XXX: Possibly a lambda impl method, which is private (static or instance). It is the easiest for us
            // to make it visible, which should be quite safe because nothing inherits the companion class.
            // The clean solution would be to generate an access method for it, but due to the location in code
            // which generates those access methods, it would require complex code changes to pass around the
            // information from one transformation to another.
            access &= ~ACC_PRIVATE;
        }
        if (isInstanceMethod(access)) {
            // default method; make static and take 'this' as the first argument
            access |= ACC_STATIC;
            // TODO: this adding of the first argument is duplicated in ClassHierarchyAnalyzer
            desc = Bytecode.prependArgumentType(desc, Type.getObjectType(interfaceName));
            signature = null; // XXX: we should prepend the argument type also to the generic signature, but removing generics is easier
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        // an interface can only contain constant fields; they don't need to be copied
        return null;
    }
}
