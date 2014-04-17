package net.orfjackal.retrolambda.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.attribute;
import static org.twdata.maven.mojoexecutor.MojoExecutor.attributes;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "process", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class RetrolambdaMojo extends AbstractMojo {

	private static final String RETROLAMBDA_JAR = "retrolambda.jar";

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Component
	private MavenProject project;

	@Parameter(required = false, property = "java8home")
	private String java8home;

	@Parameter(required = false, property = "bytecodeVersion")
	private String bytecodeVersion;

	@Parameter(required = false, property = "retrolambdaInputDir")
	private String inputDir;

	@Parameter(required = false, property = "retrolambdaInputTestDir")
	private String inputTestDir;

	@Override
	public void execute() throws MojoExecutionException {
		Log log = getLog();
		log.info("starting execution");
		String retrolambdaVersion = getRetrolambdaVersion();
		executeMojo(
				plugin(groupId("org.apache.maven.plugins"),
						artifactId("maven-dependency-plugin"), version("2.0")),
				goal("copy"),
				configuration(element(
						"artifactItems",
						element("artifactItem",
								element(name("groupId"),
										"net.orfjackal.retrolambda"),
								element(name("artifactId"), "retrolambda"),
								element(name("version"), retrolambdaVersion),
								element(name("overWrite"), "true"),
								element(name("outputDirectory"), project
										.getBuild().getDirectory()),
								element(name("destFileName"), RETROLAMBDA_JAR)))),
				executionEnvironment(project, session, pluginManager));
		log.info("copied retrolambda.jar to build directory");

		if (inputDir == null)
			inputDir = project.getBuild().getOutputDirectory();
		if (inputTestDir == null)
			inputTestDir = project.getBuild().getTestOutputDirectory();

		// process main classes
		processClasses(inputDir, "maven.compile.classpath");

		// process test classes
		processClasses(inputTestDir, "maven.test.classpath");

	}

	private void processClasses(String input, String classpathId)
			throws MojoExecutionException {
		if (bytecodeVersion == null)
			bytecodeVersion = "51";

		executeMojo(
				plugin(groupId("org.apache.maven.plugins"),
						artifactId("maven-antrun-plugin"), version("1.7")),
				goal("run"),
				configuration(element(
						"target",
						element("property",
								attributes(attribute("name", "the_classpath"),
										attribute("refid", classpathId))),
						element("exec",
								attributes(
										attribute("executable", java8home()
												+ "/bin/java"),
										attribute("failonerror", "true")),
								element("arg",
										attribute("value",
												"-Dretrolambda.bytecodeVersion="
														+ bytecodeVersion)),
								element("arg",
										attribute("value",
												"-Dretrolambda.inputDir="
														+ input)),
								element("arg",
										attribute("value",
												"-Dretrolambda.classpath=${the_classpath}")),
								element("arg",
										attribute("value", "-javaagent:"
												+ project.getBuild()
														.getDirectory() + "/"
												+ RETROLAMBDA_JAR)),
								element("arg", attribute("value", "-jar")),
								element("arg",
										attribute("value", project.getBuild()
												.getDirectory()
												+ "/"
												+ RETROLAMBDA_JAR))))),
				executionEnvironment(project, session, pluginManager));
	}

	private String java8home() {
		if (java8home != null)
			return new File(java8home).getAbsolutePath();
		else
			return System.getenv("JAVA8_HOME");
	}

	private static String getRetrolambdaVersion() {
		InputStream is = RetrolambdaMojo.class
				.getResourceAsStream("/retrolambda.properties");
		Properties p = new Properties();
		try {
			p.load(is);
			return p.getProperty("retrolambda.version");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
