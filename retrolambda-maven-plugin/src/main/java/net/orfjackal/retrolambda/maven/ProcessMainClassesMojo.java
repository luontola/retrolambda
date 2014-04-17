package net.orfjackal.retrolambda.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "process-main", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class ProcessMainClassesMojo extends ProcessClassesMojo {

	public ProcessMainClassesMojo() {
		super(ClassesType.MAIN);
	}

}
