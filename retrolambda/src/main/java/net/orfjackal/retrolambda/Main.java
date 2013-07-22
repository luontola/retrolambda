// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        System.out.println("Retrolambda " + getVersion());

        Config config = new Config(System.getProperties());
        Path inputDir = config.getInputDir();
        Path outputDir = config.getOutputDir();
        System.out.println("Input directory:  " + inputDir);
        System.out.println("Output directory: " + outputDir);

        if (!Files.isDirectory(inputDir)) {
            System.out.println("Nothing to do; not a directory: " + inputDir);
            return;
        }

        try {
            Files.walkFileTree(inputDir, new BytecodeTransformingFileVisitor(inputDir, outputDir) {
                protected byte[] transform(byte[] bytecode) {
                    return LambdaUsageBackporter.transform(bytecode);
                }
            });

        } catch (Throwable t) {
            System.out.println("Error! Failed to transform some classes");
            t.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static String getVersion() {
        Properties p = new Properties();
        try (InputStream in = ClassLoader.getSystemResourceAsStream("META-INF/maven/net.orfjackal.retrolambda/retrolambda/pom.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return p.getProperty("version", "DEVELOPMENT-VERSION");
    }
}
