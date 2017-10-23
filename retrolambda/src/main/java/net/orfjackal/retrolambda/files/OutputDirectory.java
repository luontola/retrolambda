// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.files;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.*;
import java.util.function.Predicate;

public class OutputDirectory {

    private final Path outputDir;
    private Predicate<String> classNamePredicate;

    public OutputDirectory(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void writeClass(byte[] bytecode) throws IOException {
        if (bytecode == null) {
            return;
        }
        ClassReader cr = new ClassReader(bytecode);
        String classname = cr.getClassName();
        if (classNamePredicate == null || classNamePredicate.test(classname)) {
            Path relativePath = outputDir.getFileSystem().getPath(classname + ".class");
            writeFile(relativePath, bytecode);
        }
    }

    public void writeFile(Path relativePath, byte[] content) throws IOException {
        Path outputFile = outputDir.resolve(relativePath);
        Files.createDirectories(outputFile.getParent());
        Files.write(outputFile, content);
    }

    public void setClassNamePredicate(Predicate<String> classNamePredicate) {
        this.classNamePredicate = classNamePredicate;
    }
}
