// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.Opcodes;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Main {

    private static Map<Integer, String> bytecodeVersionNames = new HashMap<>();

    static {
        bytecodeVersionNames.put(Opcodes.V1_1, "Java 1.1");
        bytecodeVersionNames.put(Opcodes.V1_2, "Java 1.2");
        bytecodeVersionNames.put(Opcodes.V1_3, "Java 1.3");
        bytecodeVersionNames.put(Opcodes.V1_4, "Java 1.4");
        bytecodeVersionNames.put(Opcodes.V1_5, "Java 5");
        bytecodeVersionNames.put(Opcodes.V1_6, "Java 6");
        bytecodeVersionNames.put(Opcodes.V1_7, "Java 7");
        bytecodeVersionNames.put(Opcodes.V1_7 + 1, "Java 8");
    }

    public static void main(String[] args) {
        System.out.println("Retrolambda " + getVersion());

        Config config = new Config(System.getProperties());
        int bytecodeVersion = config.getBytecodeVersion();
        Path inputDir = config.getInputDir();
        Path outputDir = config.getOutputDir();
        String classpath = config.getClasspath();
        System.out.println("Bytecode version: " + bytecodeVersion
                + " (" + bytecodeVersionNames.getOrDefault(bytecodeVersion, "unknown version") + ")");
        System.out.println("Input directory:  " + inputDir);
        System.out.println("Output directory: " + outputDir);
        System.out.println("Classpath:        " + classpath);

        if (!Files.isDirectory(inputDir)) {
            System.out.println("Nothing to do; not a directory: " + inputDir);
            return;
        }

        try {
            Thread.currentThread().setContextClassLoader(new URLClassLoader(asUrls(classpath)));
            Files.walkFileTree(inputDir, new BytecodeTransformingFileVisitor(inputDir, outputDir) {
                protected byte[] transform(byte[] bytecode) {
                    return LambdaUsageBackporter.transform(bytecode, bytecodeVersion);
                }
            });

        } catch (Throwable t) {
            System.out.println("Error! Failed to transform some classes");
            t.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static URL[] asUrls(String classpath) {
        String[] paths = classpath.split(System.getProperty("path.separator"));
        return Arrays.asList(paths).stream()
                .map(s -> Paths.get(s).toUri())
                .map(Main::uriToUrl)
                .toArray(URL[]::new);
    }

    private static URL uriToUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
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
