// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.nio.file.Path;
import java.util.List;

public interface Config {

    int getBytecodeVersion();

    boolean isDefaultMethodsEnabled();

    Path getInputDir();

    Path getOutputDir();

    List<Path> getClasspath();

    List<Path> getIncludedFiles();

    boolean isJavacHacksEnabled();

    boolean isQuiet();
}
