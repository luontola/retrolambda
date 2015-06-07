// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import java.io.*;
import java.util.Properties;

public class TestEnv {

    public static final File testClassesDir;

    static {
        Properties p = new Properties();
        try (InputStream in = TestEnv.class.getResourceAsStream("/testing.properties")) {
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        testClassesDir = new File(p.getProperty("testClassesDir"));
    }
}
