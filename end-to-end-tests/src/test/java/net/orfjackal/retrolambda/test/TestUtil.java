// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.objectweb.asm.ClassReader;

public class TestUtil {

    public static void assertClassExists(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Expected a class to exists, but it did not: " + className, e);
        }
    }

    public static void assertClassDoesNotExist(String className) {
        try {
            Class.forName(className);
            throw new AssertionError("Expected a class to not exists, but it did: " + className);
        } catch (ClassNotFoundException e) {
            // OK
        }
    }

    public static Class<?> companionOf(Class<?> itf) throws ClassNotFoundException {
        return Class.forName(companionNameOf(itf));
    }

    public static String companionNameOf(Class<?> itf) {
        return itf.getName() + "$";
    }

    public static void visitConstantPool(ClassReader reader, ConstantPoolVisitor visitor) {
        char[] buf = new char[reader.getMaxStringLength()];
        for (int item = 0; item < reader.getItemCount(); item++) {
            try {
                Object constant = reader.readConst(item, buf);
                visitor.visit(item, constant);
            } catch (Exception e) {
                // XXX: constant pool entry which is a Methodref, InvokeDynamic or similar non-plain constant
                // FIXME: readConst throws ArrayIndexOutOfBoundsException nearly all the time; how to use it???
                //e.printStackTrace();
            }
        }
    }

    public interface ConstantPoolVisitor {
        void visit(int item, Object constant);
    }
}
