// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class DocumentationTest {

    @Test
    public void README_contains_the_usage_instructions() throws IOException {
        String readme = toString(findInClosestParentDir("README.md"));
        String help = new SystemPropertiesConfig(new Properties()).getHelp();

        assertTrue("Expected README to contain the following text:\n\n" + help, readme.contains(help));
    }


    private static Path findInClosestParentDir(String filename) throws IOException {
        for (Path dir = Paths.get(".").toRealPath(); dir.getParent() != null; dir = dir.getParent()) {
            Path file = dir.resolve(filename);
            if (Files.exists(file)) {
                return file;
            }
        }
        throw new FileNotFoundException(filename);
    }

    private static String toString(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
