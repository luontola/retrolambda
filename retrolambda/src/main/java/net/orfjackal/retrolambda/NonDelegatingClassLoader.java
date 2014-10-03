// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.net.*;

public class NonDelegatingClassLoader extends URLClassLoader {

    public NonDelegatingClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("java.")) { // the java.* classes can only be loaded by the bootstrap class loader
            return super.loadClass(name);
        }
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name);
        }
    }
}
