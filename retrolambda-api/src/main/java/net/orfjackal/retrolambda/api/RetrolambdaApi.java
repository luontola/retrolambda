// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.api;

public class RetrolambdaApi {

    private static final String PREFIX = "retrolambda.";
    public static final String QUIET = PREFIX + "quiet";
    public static final String INCLUDED_FILES = PREFIX + "includedFiles";
    public static final String INCLUDED_FILES_FILE = INCLUDED_FILES + "File";
    public static final String CLASSPATH = PREFIX + "classpath";
    public static final String CLASSPATH_FILE = CLASSPATH + "File";
    public static final String OUTPUT_DIR = PREFIX + "outputDir";
    public static final String INPUT_DIR = PREFIX + "inputDir";
    public static final String DEFAULT_METHODS = PREFIX + "defaultMethods";
    public static final String BYTECODE_VERSION = PREFIX + "bytecodeVersion";
    public static final String JAVAC_HACKS = PREFIX + "javacHacks";
}
