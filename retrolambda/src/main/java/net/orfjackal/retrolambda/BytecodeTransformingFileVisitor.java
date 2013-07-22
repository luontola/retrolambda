// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class BytecodeTransformingFileVisitor extends SimpleFileVisitor<Path> {

    private final Path inputDir;
    private final Path outputDir;

    public BytecodeTransformingFileVisitor(Path inputDir, Path outputDir) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
    }

    @Override
    public FileVisitResult visitFile(Path inputFile, BasicFileAttributes attrs) throws IOException {
        if (isJavaClass(inputFile)) {
            byte[] originalBytes = Files.readAllBytes(inputFile);
            byte[] transformedBytes = transform(originalBytes);

            Path outputFile = outputDir.resolve(inputDir.relativize(inputFile));
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, transformedBytes);
        }
        return FileVisitResult.CONTINUE;
    }

    protected abstract byte[] transform(byte[] bytecode);

    private static boolean isJavaClass(Path file) {
        return file.getFileName().toString().endsWith(".class");
    }
}
