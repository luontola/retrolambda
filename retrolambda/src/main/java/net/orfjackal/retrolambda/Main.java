// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("Retrolambda " + getVersion());

        Path classesDir = Paths.get(args[0]);
        if (!Files.isDirectory(classesDir)) {
            System.out.println("Nothing to do; not a directory: " + classesDir);
            return;
        }

        Files.walkFileTree(classesDir, new BytecodeTransformingFileVisitor() {
            protected byte[] transform(byte[] bytecode) {
                return LambdaBackporter.transform(bytecode);
            }
        });
    }

    private static String getVersion() throws IOException {
        Properties p = new Properties();
        try (InputStream in = ClassLoader.getSystemResourceAsStream("META-INF/maven/net.orfjackal.retrolambda/retrolambda/pom.properties")) {
            if (in != null) {
                p.load(in);
            }
        }
        return p.getProperty("version", "DEVELOPMENT-VERSION");
    }
}
