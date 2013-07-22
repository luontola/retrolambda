// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.nio.file.*;
import java.util.Properties;

public class Config {

    private static final String INPUT_DIR = "retrolambda.inputDir";
    private static final String OUTPUT_DIR = "retrolambda.outputDir";

    private final Properties p;

    public Config(Properties p) {
        this.p = p;
    }

    public Path getInputDir() {
        String inputDir = p.getProperty(INPUT_DIR);
        if (inputDir == null) {
            throw new IllegalArgumentException("Missing required property: " + INPUT_DIR);
        }
        return Paths.get(inputDir);
    }

    public Path getOutputDir() {
        String outputDir = p.getProperty(OUTPUT_DIR);
        if (outputDir == null) {
            return getInputDir();
        }
        return Paths.get(outputDir);
    }
}
