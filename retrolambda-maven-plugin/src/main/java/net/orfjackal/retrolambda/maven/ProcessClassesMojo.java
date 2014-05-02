// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.maven;

import com.google.common.base.Joiner;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.*;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

abstract class ProcessClassesMojo extends AbstractMojo {

    private static final String RETROLAMBDA_JAR = "retrolambda.jar";

    private final Map<String, Integer> targetBytecodeVersions = new HashMap<String, Integer>();

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private MavenProject project;

    /**
     * The location of the Java 8 JDK (not JRE).
     */
    @Parameter(required = false, property = "java8home", defaultValue = "${env.JAVA8_HOME}")
    public String java8home;

    /**
     * The Java version targeted by the bytecode processing. Possible values are
     * 1.5, 1.6, 1.7 and 1.8. After processing the classes will be compatible
     * with the target JVM provided the known limitations are considered. See
     * <a href="https://github.com/orfjackal/retrolambda">project documentation</a>
     * for more details.
     */
    @Parameter(required = false, property = "retrolambdaTarget", defaultValue = "1.7")
    public String target;

    /**
     * The directory containing the main (non-test) compiled classes. These
     * classes will be overwritten with bytecode changes to obtain compatibility
     * with target Java runtime.
     */
    @Parameter(required = false, property = "retrolambdaMainClassesDir", defaultValue = "${project.build.outputDirectory}")
    public String mainClassesDir;

    /**
     * The directory containing the compiled test classes. These classes will be
     * overwritten with bytecode changes to obtain compatibility with target
     * Java runtime.
     */
    @Parameter(required = false, property = "retrolambdaTestClassesDir", defaultValue = "${project.build.testOutputDirectory}")
    public String testClassesDir;

    private final ClassesType classesType;

    ProcessClassesMojo(ClassesType classesType) {
        this.classesType = classesType;
        targetBytecodeVersions.put("1.5", 49);
        targetBytecodeVersions.put("1.6", 50);
        targetBytecodeVersions.put("1.7", 51);
        targetBytecodeVersions.put("1.8", 52);
    }

    @Override
    public void execute() throws MojoExecutionException {
        validateJava8home();
        validateTarget();

        getLog().info("Retrieving the Retrolambda JAR");
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.0")),
                goal("copy"),
                configuration(element("artifactItems",
                        element("artifactItem",
                                element(name("groupId"), "net.orfjackal.retrolambda"),
                                element(name("artifactId"), "retrolambda"),
                                element(name("version"), getRetrolambdaVersion()),
                                element(name("overWrite"), "true"),
                                element(name("outputDirectory"), project.getBuild().getDirectory()),
                                element(name("destFileName"), RETROLAMBDA_JAR)))),
                executionEnvironment(project, session, pluginManager));

        getLog().info("Processing classes with Retrolambda");
        if (classesType == ClassesType.MAIN) {
            processClasses(mainClassesDir, "maven.compile.classpath");
        } else {
            processClasses(testClassesDir, "maven.test.classpath");
        }
    }

    private void validateTarget() throws MojoExecutionException {
        if (!targetBytecodeVersions.containsKey(target)) {
            String possibleValues = Joiner.on(", ").join(new ArrayList<String>(targetBytecodeVersions.keySet()));
            throw new MojoExecutionException(
                    "Unrecognized target '" + target + "'. Possible values are " + possibleValues);
        }
    }

    private void validateJava8home() throws MojoExecutionException {
        if (!new File(java8home).isDirectory()) {
            throw new MojoExecutionException(
                    "Must set configuration element java8home or environment variable JAVA8_HOME to a valid JDK 8 location: " + java8home);
        }
    }

    private void processClasses(String inputDir, String classpathId) throws MojoExecutionException {
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"),
                        artifactId("maven-antrun-plugin"),
                        version("1.7")),
                goal("run"),
                configuration(element(
                        "target",
                        element("property",
                                attributes(attribute("name", "the_classpath"),
                                        attribute("refid", classpathId))),
                        element("exec",
                                attributes(
                                        attribute("executable", java8home + "/bin/java"),
                                        attribute("failonerror", "true")),
                                element("arg", attribute("value", "-Dretrolambda.bytecodeVersion=" + targetBytecodeVersions.get(target))),
                                element("arg", attribute("value", "-Dretrolambda.inputDir=" + inputDir)),
                                element("arg", attribute("value", "-Dretrolambda.classpath=${the_classpath}")),
                                element("arg", attribute("value", "-javaagent:" + project.getBuild().getDirectory() + "/" + RETROLAMBDA_JAR)),
                                element("arg", attribute("value", "-jar")),
                                element("arg", attribute("value", project.getBuild().getDirectory() + "/" + RETROLAMBDA_JAR))))),
                executionEnvironment(project, session, pluginManager));
    }

    private static String getRetrolambdaVersion() throws MojoExecutionException {
        try {
            InputStream is = ProcessClassesMojo.class.getResourceAsStream(
                    "/META-INF/maven/net.orfjackal.retrolambda/retrolambda-maven-plugin/pom.properties");
            try {
                Properties p = new Properties();
                p.load(is);
                return p.getProperty("version");
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to detect the Retrolambda version", e);
        }
    }
}
