// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.*;

import java.io.File;
import java.util.List;

/**
 * Processes main classes compiled with Java 8 so that they will be compatible with
 * Java 5, 6 or 7 runtime.
 */
@Mojo(name = "process-main",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class ProcessMainClassesMojo extends ProcessClassesMojo {

    /**
     * Directory containing the original classes compiled with Java 8.
     *
     * @since 1.3.0
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "retrolambdaMainInputDir", required = true)
    public File mainInputDir;

    /**
     * Directory where to write the backported classes.
     * If same as the input directory, will overwrite the original classes.
     *
     * @since 1.3.0
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "retrolambdaMainOutputDir", required = true)
    public File mainOutputDir;

    @Override
    protected File getInputDir() {
        return mainInputDir;
    }

    @Override
    protected File getOutputDir() {
        return mainOutputDir;
    }

    @Override
    protected List<String> getClasspathElements() throws DependencyResolutionRequiredException {
        return project.getCompileClasspathElements();
    }
}
