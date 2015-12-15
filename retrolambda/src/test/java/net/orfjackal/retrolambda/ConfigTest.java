// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import com.google.common.base.*;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConfigTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final Properties systemProperties = new Properties();

    private Config config() {
        return new Config(systemProperties);
    }

    @Test
    public void bytecode_version() {
        assertThat("defaults to Java 7", config().getBytecodeVersion(), is(51));
        assertThat("human printable format", config().getJavaVersion(), is("Java 7"));

        systemProperties.setProperty(Config.BYTECODE_VERSION, "50");
        assertThat("can override the default", config().getBytecodeVersion(), is(50));
        assertThat("human printable format", config().getJavaVersion(), is("Java 6"));
    }

    @Test
    public void default_methods() {
        assertThat("defaults to disabled", config().isDefaultMethodsEnabled(), is(false));

        systemProperties.setProperty(Config.DEFAULT_METHODS, "true");
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
        systemProperties.setProperty(Config.INPUT_DIR, "input dir");
        assertThat("defaults to input dir", config().getOutputDir(), is(Paths.get("input dir")));

        systemProperties.setProperty(Config.OUTPUT_DIR, "output dir");
        assertThat("can override the default", config().getOutputDir(), is(Paths.get("output dir")));
    }

    @Test
    public void included_files() {
        assertThat("not set", config().getIncludedFiles(), is(nullValue()));

        systemProperties.setProperty(Config.INCLUDED_FILES, "");
        assertThat("zero values", config().getIncludedFiles(), is(empty()));

        systemProperties.setProperty(Config.INCLUDED_FILES, "/foo/one.class");
        assertThat("one value", config().getIncludedFiles(), is(Arrays.asList(Paths.get("/foo/one.class"))));

        systemProperties.setProperty(Config.INCLUDED_FILES, "/foo/one.class" + File.pathSeparator + "/foo/two.class");
        assertThat("multiple values", config().getIncludedFiles(), is(Arrays.asList(Paths.get("/foo/one.class"), Paths.get("/foo/two.class"))));
    }

    @Test
    public void included_file() throws IOException {
        assertThat("not set", config().getIncludedFileList(), is(nullValue()));

        systemProperties.setProperty(Config.INCLUDED_FILE, "");
        assertThat("zero values", config().getIncludedFileList(), is(empty()));

        // Single file
        File singleTmp = File.createTempFile("test",".list");
        singleTmp.deleteOnExit();

        List<String> file = Lists.newArrayList("foo.java");
        String delimiter = System.getProperty("line.separator");
        Files.write(file.stream()
                .collect(Collectors.joining(delimiter)), singleTmp, Charsets.UTF_8);

        systemProperties.setProperty(Config.INCLUDED_FILE, singleTmp.getAbsolutePath());
        assertThat("one value", config().getIncludedFileList(), is(
                file.stream()
                        .map(f -> Paths.get(f))
                        .collect(Collectors.toList())));

        // Multiple files
        File multiTmp = File.createTempFile("test",".list");
        multiTmp.deleteOnExit();

        List<String> files = Lists.newArrayList("foo.java", "bar.java");
        Files.write(files.stream()
                .collect(Collectors.joining(delimiter)), multiTmp, Charsets.UTF_8);

        systemProperties.setProperty(Config.INCLUDED_FILE, multiTmp.getAbsolutePath());
        assertThat("two values", config().getIncludedFileList(), is(
                files.stream()
                        .map(f -> Paths.get(f))
                        .collect(Collectors.toList())));
    }
}