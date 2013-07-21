// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class BytecodeTransformingFileVisitor extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (isJavaClass(file)) {
            System.out.println(file);
            byte[] originalBytes = Files.readAllBytes(file);
            byte[] transformedBytes = transform(originalBytes);
            Files.write(file, transformedBytes);
        }
        return FileVisitResult.CONTINUE;
    }

    protected abstract byte[] transform(byte[] bytecode);

    private static boolean isJavaClass(Path file) {
        return file.getFileName().toString().endsWith(".class");
    }
}
