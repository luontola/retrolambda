// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.files;

import net.orfjackal.retrolambda.ext.ow2asm.EnhancedClassReader;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.*;

public class OutputDirectory {

    private final Path outputDir;

    public OutputDirectory(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void writeClass(byte[] bytecode, boolean isJavacHacksEnabled) throws IOException {
        if (bytecode == null) {
            return;
        }
        ClassReader cr = EnhancedClassReader.create(bytecode, isJavacHacksEnabled);
        Path relativePath = outputDir.getFileSystem().getPath(cr.getClassName() + ".class");
        writeFile(relativePath, bytecode);
    }

    public void writeFile(Path relativePath, byte[] content) throws IOException {
        Path outputFile = outputDir.resolve(relativePath);
        Files.createDirectories(outputFile.getParent());
        Files.write(outputFile, content);
    }
}
