package net.orfjackal.retrolambda.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Processes test classes compiled with java 8 so that they are compatible with
 * java 7 runtime.
 */
@Mojo(name = "process-test", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES)
public class ProcessTestClassesMojo extends ProcessClassesMojo {

	public ProcessTestClassesMojo() {
		super(ClassesType.TEST);
	}
}