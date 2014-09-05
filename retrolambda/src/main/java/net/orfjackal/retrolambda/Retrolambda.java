// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Retrolambda {

    public static void run(Config config) throws Throwable {
        int bytecodeVersion = config.getBytecodeVersion();
        Path inputDir = config.getInputDir();
        Path outputDir = config.getOutputDir();
        String classpath = config.getClasspath();
        List<Path> includedFiles = config.getIncludedFiles();
        System.out.println("Bytecode version: " + bytecodeVersion + " (" + config.getJavaVersion() + ")");
        System.out.println("Input directory:  " + inputDir);
        System.out.println("Output directory: " + outputDir);
        System.out.println("Classpath:        " + classpath);
        if (includedFiles != null) {
            System.out.println("Included files:   " + includedFiles.size());
        }

        if (!Files.isDirectory(inputDir)) {
            System.out.println("Nothing to do; not a directory: " + inputDir);
            return;
        }

        Thread.currentThread().setContextClassLoader(new NonDelegatingClassLoader(asUrls(classpath)));

        try (LambdaClassDumper dumper = new LambdaClassDumper(new LambdaClassSaver(outputDir, bytecodeVersion))) {
            if (!PreMain.isAgentLoaded()) {
                dumper.install();
            }

            ClassHierarchyAnalyzer analyzer = new ClassHierarchyAnalyzer();
            visitFiles(inputDir, includedFiles, new BytecodeFileVisitor() {
                @Override
                protected void visit(byte[] bytecode) {
                    analyzer.analyze(bytecode);
                }
            });

            ClassSaver saver = new ClassSaver(outputDir);
            for (ClassReader reader : analyzer.getInterfaces()) {
                saver.save(LambdaUsageBackporter.transform(reader, bytecodeVersion));
            }
            for (ClassReader reader : analyzer.getClasses()) {
                saver.save(LambdaUsageBackporter.transform(reader, bytecodeVersion));
            }
        }
    }

    static void visitFiles(Path inputDir, List<Path> includedFiles, FileVisitor<Path> visitor) throws IOException {
        if (includedFiles != null) {
            visitor = new FilteringFileVisitor(includedFiles, visitor);
        }
        Files.walkFileTree(inputDir, visitor);
    }

    private static URL[] asUrls(String classpath) {
        String[] paths = classpath.split(System.getProperty("path.separator"));
        return Arrays.asList(paths).stream()
                .map(s -> Paths.get(s).toUri())
                .map(Retrolambda::uriToUrl)
                .toArray(URL[]::new);
    }

    private static URL uriToUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
