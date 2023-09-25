// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.files;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FilteringFileVisitor implements FileVisitor<Path> {

    private final Set<Path> fileFilter;
    private final FileVisitor<? super Path> target;

    public FilteringFileVisitor(Collection<Path> fileFilter, FileVisitor<Path> target) {
        this.fileFilter = fileFilter == null ? null : new HashSet<>(fileFilter);
        this.target = target;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return target.postVisitDirectory(dir, exc);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (dir.toString().endsWith("/META-INF/versions")) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return target.preVisitDirectory(dir, attrs);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (fileFilter == null || fileFilter.contains(file)) {
            return target.visitFile(file, attrs);
        } else {
            return FileVisitResult.CONTINUE;
        }
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return target.visitFileFailed(file, exc);
    }
}
