// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.util.Bytecode;
import org.junit.*;
import org.junit.rules.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SystemPropertiesConfigTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    private final Properties systemProperties = new Properties();

    private SystemPropertiesConfig config() {
        return new SystemPropertiesConfig(systemProperties);
    }

    @Test
    public void is_fully_configured_when_required_properties_are_set() {
        assertThat("before", config().isFullyConfigured(), is(false));

        systemProperties.setProperty(SystemPropertiesConfig.INPUT_DIR, "");
        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH, "");

        assertThat("after", config().isFullyConfigured(), is(true));
    }

    @Test
    public void can_use_alternative_parameter_instead_of_required_parameter() {
        systemProperties.setProperty(SystemPropertiesConfig.INPUT_DIR, "");
        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH_FILE, "");

        assertThat("is fully configured?", config().isFullyConfigured(), is(true));
    }

    @Test
    public void bytecode_version() {
        assertThat("defaults to Java 7", config().getBytecodeVersion(), is(51));
        assertThat("human printable format", Bytecode.getJavaVersion(config().getBytecodeVersion()), is("Java 7"));

        systemProperties.setProperty(SystemPropertiesConfig.BYTECODE_VERSION, "50");
        assertThat("can override the default", config().getBytecodeVersion(), is(50));
        assertThat("human printable format", Bytecode.getJavaVersion(config().getBytecodeVersion()), is("Java 6"));
    }

    @Test
    public void default_methods() {
        assertThat("defaults to disabled", config().isDefaultMethodsEnabled(), is(false));

        systemProperties.setProperty(SystemPropertiesConfig.DEFAULT_METHODS, "true");
        assertThat("can override the default", config().isDefaultMethodsEnabled(), is(true));
    }

    @Test
    public void input_directory_is_required() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing required property: retrolambda.inputDir");
        config().getInputDir();
    }

    @Test
    public void output_directory() {
        systemProperties.setProperty(SystemPropertiesConfig.INPUT_DIR, "input dir");
        assertThat("defaults to input dir", config().getOutputDir(), is(Paths.get("input dir")));

        systemProperties.setProperty(SystemPropertiesConfig.OUTPUT_DIR, "output dir");
        assertThat("can override the default", config().getOutputDir(), is(Paths.get("output dir")));
    }

    @Test
    public void classpath() {
        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH, "");
        assertThat("zero values", config().getClasspath(), is(empty()));

        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH, "one.jar");
        assertThat("one value", config().getClasspath(), is(Arrays.asList(Paths.get("one.jar"))));

        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH, "one.jar" + File.pathSeparator + "two.jar");
        assertThat("multiple values", config().getClasspath(), is(Arrays.asList(Paths.get("one.jar"), Paths.get("two.jar"))));
    }

    @Test
    public void classpath_file() throws IOException {
        Path file = tempDir.newFile("classpath.txt").toPath();

        Files.write(file, Arrays.asList("", "", "")); // empty lines are ignored
        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH_FILE, file.toString());
        assertThat("zero values", config().getClasspath(), is(empty()));

        Files.write(file, Arrays.asList("one.jar"));
        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH_FILE, file.toString());
        assertThat("one value", config().getClasspath(), is(Arrays.asList(Paths.get("one.jar"))));

        Files.write(file, Arrays.asList("one.jar", "two.jar"));
        systemProperties.setProperty(SystemPropertiesConfig.CLASSPATH_FILE, file.toString());
        assertThat("multiple values", config().getClasspath(), is(Arrays.asList(Paths.get("one.jar"), Paths.get("two.jar"))));
    }

    @Test
    public void classpath_is_required() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing required property: retrolambda.classpath");
        config().getClasspath();
    }

    @Test
    public void included_files() {
        assertThat("not set", config().getIncludedFiles(), is(nullValue()));

        systemProperties.setProperty(SystemPropertiesConfig.INCLUDED_FILES, "");
        assertThat("zero values", config().getIncludedFiles(), is(empty()));

        systemProperties.setProperty(SystemPropertiesConfig.INCLUDED_FILES, "/foo/one.class");
        assertThat("one value", config().getIncludedFiles(), is(Arrays.asList(Paths.get("/foo/one.class"))));

        systemProperties.setProperty(SystemPropertiesConfig.INCLUDED_FILES, "/foo/one.class" + File.pathSeparator + "/foo/two.class");
        assertThat("multiple values", config().getIncludedFiles(), is(Arrays.asList(Paths.get("/foo/one.class"), Paths.get("/foo/two.class"))));
    }

    @Test
    public void included_files_file() throws IOException {
        Path file = tempDir.newFile("includedFiles.txt").toPath();
        assertThat("not set", config().getIncludedFiles(), is(nullValue()));

        Files.write(file, Arrays.asList("", "", "")); // empty lines are ignored
        systemProperties.setProperty(SystemPropertiesConfig.INCLUDED_FILES_FILE, file.toString());
        assertThat("zero values", config().getIncludedFiles(), is(empty()));

        Files.write(file, Arrays.asList("one.class"));
        systemProperties.setProperty(SystemPropertiesConfig.INCLUDED_FILES_FILE, file.toString());
        assertThat("one value", config().getIncludedFiles(), is(Arrays.asList(Paths.get("one.class"))));

        Files.write(file, Arrays.asList("one.class", "two.class"));
        systemProperties.setProperty(SystemPropertiesConfig.INCLUDED_FILES_FILE, file.toString());
        assertThat("multiple values", config().getIncludedFiles(), is(Arrays.asList(Paths.get("one.class"), Paths.get("two.class"))));
    }
}
