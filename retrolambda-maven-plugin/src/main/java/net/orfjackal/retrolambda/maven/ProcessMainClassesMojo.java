// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.maven;

import org.apache.maven.plugins.annotations.*;

/**
 * Processes main (non-test) classes compiled with Java 8 so that they will be
 * compatible with Java 5, 6 or 7 runtime.
 */
@Mojo(name = "process-main",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class ProcessMainClassesMojo extends ProcessClassesMojo {

    public ProcessMainClassesMojo() {
        super(ClassesType.MAIN);
    }
}
