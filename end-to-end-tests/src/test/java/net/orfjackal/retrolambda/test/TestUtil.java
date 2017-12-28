// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.apache.bcel.classfile.*;

import java.io.*;

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

    public static ConstantPool getConstantPool(String className) throws IOException {
        String fileName = className + ".class";
        try (InputStream in = TestUtil.class.getResourceAsStream("/" + fileName)) {
            ClassParser parser = new ClassParser(in, className);
            JavaClass javaClass = parser.parse();
            return javaClass.getConstantPool();
        }
    }
}
