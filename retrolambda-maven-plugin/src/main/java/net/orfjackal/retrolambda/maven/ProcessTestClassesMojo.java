// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.maven;

import org.apache.maven.plugins.annotations.*;

/**
 * Processes test classes compiled with Java 8 so that they will be compatible with
 * Java 5, 6 or 7 runtime.
 */
@Mojo(name = "process-test",
        defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ProcessTestClassesMojo extends ProcessClassesMojo {

    public ProcessTestClassesMojo() {
        super(ClassesType.TEST);
    }
}