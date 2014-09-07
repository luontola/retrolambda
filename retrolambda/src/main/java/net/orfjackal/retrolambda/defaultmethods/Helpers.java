// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.defaultmethods;

import net.orfjackal.retrolambda.Config;
import org.objectweb.asm.Type;

import java.io.File;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.*;

import static org.objectweb.asm.Type.*;

/**
 * Created by arneball on 2014-08-12.
 */
public class Helpers {
    private static final Pattern pattern = Pattern.compile("\\((.*)\\)(.*)");

    public static String addParam(String desc, String className) {
        Matcher m = pattern.matcher(desc);
        m.find();
        String rest = m.group(1);
        String returntype = m.group(2);
        return String.format("(L%s;%s)%s", className, rest, returntype);
    }

    public static String changeReturnType(String desc, String returnType) {
        Matcher m = pattern.matcher(desc);
        m.find();
        String rest = m.group(1);
        return String.format("(%s)L%s;", rest, returnType);
    }

    public static Class<?> loadClass(String className) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return cl.loadClass(className.replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean isPrimitive(Type containerReturnType) {
        return Stream.of(BYTE_TYPE, SHORT_TYPE, INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, VOID_TYPE, BOOLEAN_TYPE)
                .filter(containerReturnType::equals)
                .findAny()
                .isPresent();

    }

    public static boolean interfaceBelongsToUs(String iff) {
        return InputFilesClassLoader.INSTANCE.isOurGuy(iff.replace("/", "."));
    }

    public static boolean declaringClassBelongsToInputFiles(Method method) {
        boolean ourGuy = InputFilesClassLoader.INSTANCE.isOurGuy(method.getDeclaringClass().getName());
        System.out.println("Method: " + method + ", declaring class: " + method.getDeclaringClass() + ", our guy: " + ourGuy);
        return ourGuy;
    }

    private static class InputFilesClassLoader extends URLClassLoader {
        private static final InputFilesClassLoader INSTANCE = create();
        private InputFilesClassLoader(URL inputFiles) {
            super(new URL[]{ inputFiles });
        }

        private static InputFilesClassLoader create() {
            Config config = new Config(System.getProperties());
            System.out.println("INCLUDED FILES " + config.getInputDir());
            try {
                URL inputFiles = config.getInputDir().toUri().toURL();
                return new InputFilesClassLoader(inputFiles);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        public boolean isOurGuy(String cname) {
            try {
                findSystemClass(cname);
                return false;
            } catch (ClassNotFoundException e) {
                try {
                    findClass(cname);
                    return true;
                } catch (ClassNotFoundException e1) {
                    return false;
                }
            }
        }
    }
}
