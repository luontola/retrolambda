package net.orfjackal.retrolambda.maven;

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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

abstract class ProcessClassesMojo extends AbstractMojo {

	private static final String VERSION_ANTRUN = "1.7";

	private static final String ARTIFACT_ID_ANTRUN = "maven-antrun-plugin";

	private static final String GROUP_ID_ANTRUN = "org.apache.maven.plugins";

	private static final String RETROLAMBDA_JAR = "retrolambda.jar";

	private static final int DEFAULT_BYTE_CODE_VERSION = 51;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Component
	private MavenProject project;

	/**
	 * The location of the java 8 jdk (not jre).
	 */
	@Parameter(required = false, property = "java8home", defaultValue = "${env.JAVA8_HOME}")
	private String java8home;

	/**
	 * The version of the bytecode to be produced by retrolamda. Defaults to 51
	 * which is java 7 compatible bytecode.
	 */
	@Parameter(required = false, property = "bytecodeVersion", defaultValue = DEFAULT_BYTE_CODE_VERSION
			+ "")
	private String bytecodeVersion;

	/**
	 * The directory containing the main (non-test) compiled classes. These
	 * classes will be overwritten with bytecode changes to obtain compatibility
	 * with java 7 runtime.
	 */
	@Parameter(required = false, property = "retrolambdaMainClassesDir", defaultValue = "${project.build.outputDirectory}")
	private String mainClassesDir;

	/**
	 * The directory containing the compiled test classes. These classes will be
	 * overwritten with bytecode changes to obtain compatibility with java 7
	 * runtime.
	 */
	@Parameter(required = false, property = "retrolambdaTestClassesDir", defaultValue = "${project.build.testOutputDirectory}")
	private String testClassesDir;

	private final ClassesType classesType;

	/**
	 * Constructor.
	 * 
	 * @param classesType
	 */
	ProcessClassesMojo(ClassesType classesType) {
		this.classesType = classesType;
	}

	@Override
	public void execute() throws MojoExecutionException {
		Log log = getLog();
		log.info("starting execution");
		validateJava8home();
		String retrolambdaVersion = getRetrolambdaVersion();
		executeMojo(
				plugin(groupId(GROUP_ID_ANTRUN),
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
		log.info("processing classes");
		if (classesType == ClassesType.MAIN)
			processClasses(mainClassesDir, "maven.compile.classpath");
		else
			processClasses(testClassesDir, "maven.test.classpath");
		log.info("processed classes");
	}

	private void validateJava8home() throws MojoExecutionException {
		File jdk = new File(java8home);
		if (!jdk.exists() || !jdk.isDirectory()) {
			throw new MojoExecutionException(
					"must set configuration element java8home or environment variable JAVA8_HOME to a valid jdk 8 location");
		}
	}

	private void processClasses(String input, String classpathId)
			throws MojoExecutionException {

		executeMojo(
				plugin(groupId(GROUP_ID_ANTRUN),
						artifactId(ARTIFACT_ID_ANTRUN), version(VERSION_ANTRUN)),
				goal("run"),
				configuration(element(
						"target",
						element("property",
								attributes(attribute("name", "the_classpath"),
										attribute("refid", classpathId))),
						element("exec",
								attributes(
										attribute("executable", java8home
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

	private static String getRetrolambdaVersion() {
		InputStream is = ProcessClassesMojo.class
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
