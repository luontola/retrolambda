package net.orfjackal.retrolambda.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Processes main (non-test) classes compiled with java 8 so that they are
 * compatible with java 7 runtime.
 */
@Mojo(name = "process-main", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class ProcessMainClassesMojo extends ProcessClassesMojo {

    public ProcessMainClassesMojo() {
        super(ClassesType.MAIN);
    }

}
