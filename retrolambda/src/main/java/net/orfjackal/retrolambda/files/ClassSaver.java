// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.files;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.*;

public class ClassSaver {

    private final Path outputDir;

    public ClassSaver(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void save(byte[] bytecode) throws IOException {
        if (bytecode == null) {
            return;
        }
        ClassReader cr = new ClassReader(bytecode);
        Path outputFile = outputDir.resolve(cr.getClassName() + ".class");
        Files.createDirectories(outputFile.getParent());
        Files.write(outputFile, bytecode);
    }
}
