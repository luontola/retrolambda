// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class DocumentationTest {

    @Test
    public void README_contains_the_usage_instructions() {
        String readme = toString(Paths.get("README.md"));
        String help = new Config(new Properties()).getHelp();

        assertTrue("Expected README to contain the following text:\n\n" + help, readme.contains(help));
    }

    private static String toString(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
