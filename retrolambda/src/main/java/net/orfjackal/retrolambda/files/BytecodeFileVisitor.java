// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.files;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class BytecodeFileVisitor extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult visitFile(Path inputFile, BasicFileAttributes attrs) throws IOException {
        if (isJavaClass(inputFile)) {
            visit(Files.readAllBytes(inputFile));
        }
        return FileVisitResult.CONTINUE;
    }

    protected abstract void visit(byte[] bytecode);

    private static boolean isJavaClass(Path file) {
        return file.getFileName().toString().endsWith(".class");
    }
}
