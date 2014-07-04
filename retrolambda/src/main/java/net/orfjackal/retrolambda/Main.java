// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("Retrolambda " + getVersion());

        Config config = new Config(System.getProperties());
        if (!config.isFullyConfigured()) {
            System.out.print(config.getHelp());
            return;
        }
        int bytecodeVersion = config.getBytecodeVersion();
        Path inputDir = config.getInputDir();
        Path outputDir = config.getOutputDir();
        String classpath = config.getClasspath();
        List<Path> changedFiles = config.getChangedFiles();
        System.out.println("Bytecode version: " + bytecodeVersion + " (" + config.getJavaVersion() + ")");
        System.out.println("Input directory:  " + inputDir);
        System.out.println("Output directory: " + outputDir);
        System.out.println("Classpath:        " + classpath);
        if (changedFiles != null) {
            System.out.println("Changed:          " + changedFiles.size() + " files");
        }

        if (!Files.isDirectory(inputDir)) {
            System.out.println("Nothing to do; not a directory: " + inputDir);
            return;
        }

        try {
            Thread.currentThread().setContextClassLoader(new URLClassLoader(asUrls(classpath)));

            BytecodeTransformingFileVisitor visitor = new BytecodeTransformingFileVisitor(inputDir, outputDir) {
                @Override
                protected byte[] transform(byte[] bytecode) {
                    return LambdaUsageBackporter.transform(bytecode, bytecodeVersion);
                }
            };

            if (changedFiles == null) {
                Files.walkFileTree(inputDir, visitor);
            } else {
                for (Path inputFile : changedFiles) {
                    visitor.visitFile(inputFile, Files.readAttributes(inputFile, BasicFileAttributes.class));
                }
            }
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
