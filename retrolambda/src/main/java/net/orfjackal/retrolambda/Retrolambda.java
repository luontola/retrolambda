// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.files.*;
import net.orfjackal.retrolambda.interfaces.*;
import net.orfjackal.retrolambda.lambdas.*;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Retrolambda {

    public static void run(Config config) throws Throwable {
        int bytecodeVersion = config.getBytecodeVersion();
        boolean defaultMethodsEnabled = config.isDefaultMethodsEnabled();
        Path inputDir = config.getInputDir();
        Path outputDir = config.getOutputDir();
        String classpath = config.getClasspath();
        List<Path> includedFiles = config.getIncludedFiles();
        System.out.println("Bytecode version: " + bytecodeVersion + " (" + config.getJavaVersion() + ")");
        System.out.println("Default methods:  " + defaultMethodsEnabled);
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

        ClassHierarchyAnalyzer analyzer = new ClassHierarchyAnalyzer();
        ClassSaver saver = new ClassSaver(outputDir);
        Transformers transformers = new Transformers(bytecodeVersion, defaultMethodsEnabled, analyzer);
        LambdaClassSaver lambdaClassSaver = new LambdaClassSaver(saver, transformers);

        try (LambdaClassDumper dumper = new LambdaClassDumper(lambdaClassSaver)) {
            if (PreMain.isAgentLoaded()) {
                PreMain.setLambdaClassSaver(lambdaClassSaver);
            } else {
                dumper.install();
            }

            visitFiles(inputDir, includedFiles, new BytecodeFileVisitor() {
                @Override
                protected void visit(byte[] bytecode) {
                    analyzer.analyze(bytecode);
                }
            });

            // Because Transformers.backportLambdaClass() analyzes the lambda class,
            // adding it to the analyzer's list of classes, we must take care to
            // use the list of classes before that happened, or else we might accidentally
            // overwrite the lambda class.
            List<ClassInfo> interfaces = analyzer.getInterfaces();
            List<ClassInfo> classes = analyzer.getClasses();

            List<byte[]> transformed = new ArrayList<>();
            for (ClassInfo c : interfaces) {
                transformed.addAll(transformers.backportInterface(c.reader));
            }
            for (ClassInfo c : classes) {
                transformed.add(transformers.backportClass(c.reader));
            }

            // We need to load some of the classes (for calling the lambda metafactory)
            // so we need to take care not to modify any bytecode before loading them.
            for (byte[] bytecode : transformed) {
                saver.save(bytecode);
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
