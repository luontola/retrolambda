// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.maven;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.*;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

abstract class ProcessClassesMojo extends AbstractMojo {

    private static final Map<String, Integer> targetBytecodeVersions = ImmutableMap.of(
            "1.5", 49,
            "1.6", 50,
            "1.7", 51,
            "1.8", 52
    );

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private MavenProject project;

    /**
     * The location of the Java 8 JDK (not JRE).
     *
     * @since 1.2.0
     */
    @Parameter(defaultValue = "${java.home}", property = "java8home", required = true)
    public File java8home;

    /**
     * The Java version targeted by the bytecode processing. Possible values are
     * 1.5, 1.6, 1.7 and 1.8. After processing the classes will be compatible
     * with the target JVM provided the known limitations are considered. See
     * <a href="https://github.com/orfjackal/retrolambda">project documentation</a>
     * for more details.
     *
     * @since 1.2.0
     */
    @Parameter(defaultValue = "1.7", property = "retrolambdaTarget", required = true)
    public String target;

    private final ClassesType classesType;

    ProcessClassesMojo(ClassesType classesType) {
        this.classesType = classesType;
    }

    protected abstract File getInputDir();

    protected abstract File getOutputDir();

    @Override
    public void execute() throws MojoExecutionException {
        validateJava8home();
        validateTarget();

        // TODO: use Maven's built-in artifact resolving, so that we can refer to retrolambda.jar in the local repository without copying it
        String version = getRetrolambdaVersion();
        getLog().info("Retrieving Retrolambda " + version);
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.8")),
                goal("copy"),
                configuration(element("artifactItems",
                        element("artifactItem",
                                element(name("groupId"), "net.orfjackal.retrolambda"),
                                element(name("artifactId"), "retrolambda"),
                                element(name("version"), version),
                                element(name("overWrite"), "true"),
                                element(name("outputDirectory"), getRetrolambdaJarDir()),
                                element(name("destFileName"), getRetrolambdaJarName())))),
                executionEnvironment(project, session, pluginManager));

        getLog().info("Processing classes with Retrolambda");
        if (classesType == ClassesType.MAIN) {
            processClasses("maven.compile.classpath");
        } else {
            processClasses("maven.test.classpath");
        }
    }

    private void validateTarget() throws MojoExecutionException {
        if (!targetBytecodeVersions.containsKey(target)) {
            String possibleValues = Joiner.on(", ").join(new TreeSet<String>(targetBytecodeVersions.keySet()));
            throw new MojoExecutionException(
                    "Unrecognized target '" + target + "'. Possible values are " + possibleValues);
        }
    }

    private void validateJava8home() throws MojoExecutionException {
        if (!java8home.isDirectory()) {
            throw new MojoExecutionException(
                    "Must set configuration element java8home or environment variable JAVA8_HOME to a valid JDK 8 location: " + java8home);
        }
    }

    private void processClasses(String classpathId) throws MojoExecutionException {
        String retrolambdaJar = getRetrolambdaJarPath();
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
                                element("arg", attribute("value", "-Dretrolambda.inputDir=" + getInputDir().getAbsolutePath())),
                                element("arg", attribute("value", "-Dretrolambda.outputDir=" + getOutputDir().getAbsolutePath())),
                                element("arg", attribute("value", "-Dretrolambda.classpath=${the_classpath}")),
                                element("arg", attribute("value", "-javaagent:" + retrolambdaJar)),
                                element("arg", attribute("value", "-jar")),
                                element("arg", attribute("value", retrolambdaJar))))),
                executionEnvironment(project, session, pluginManager));
    }

    private String getRetrolambdaJarPath() {
        return getRetrolambdaJarDir() + "/" + getRetrolambdaJarName();
    }

    private String getRetrolambdaJarDir() {
        return project.getBuild().getDirectory() + "/retrolambda";
    }

    private String getRetrolambdaJarName() {
        return "retrolambda.jar";
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
