// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.files;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class BytecodeFileVisitor extends SimpleFileVisitor<Path> {

    private Path baseDir;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (baseDir == null) {
            baseDir = dir;
        }
        return super.preVisitDirectory(dir, attrs);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relativePath = baseDir.relativize(file);
        byte[] content = Files.readAllBytes(file);

        if (isJavaClass(relativePath)) {
            visitClass(content);
        } else {
            visitResource(relativePath, content);
        }
        return FileVisitResult.CONTINUE;
    }

    protected abstract void visitClass(byte[] bytecode) throws IOException;

    protected abstract void visitResource(Path relativePath, byte[] content) throws IOException;

    private static boolean isJavaClass(Path file) {
        return file.getFileName().toString().endsWith(".class");
    }
}
