// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import com.google.common.collect.ImmutableMap;
import net.orfjackal.retrolambda.api.RetrolambdaApi;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertTrue;

public class RetrolambdaTest {

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    private Path inputDir;
    private Path outputDir;

    private final List<Path> visitedFiles = new ArrayList<>();
    private final FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            visitedFiles.add(file);
            return FileVisitResult.CONTINUE;
        }
    };
    private Path file1;
    private Path file2;
    private Path fileInSubdir;
    private Path outsider;
    private Path jar;
    private Path fileInJar;

    @Before
    public void setup() throws IOException {
        inputDir = tempDir.newFolder("inputDir").toPath();
        outputDir = tempDir.newFolder("outputDir").toPath();
        file1 = Files.createFile(inputDir.resolve("file1.txt"));
        file2 = Files.createFile(inputDir.resolve("file2.txt"));
        Path subdir = inputDir.resolve("subdir");
        Files.createDirectory(subdir);
        fileInSubdir = Files.createFile(subdir.resolve("file.txt"));
        outsider = tempDir.newFile("outsider.txt").toPath();
        jar = new File(tempDir.getRoot(), "file.jar").toPath();
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(URI.create("jar:file:" + jar.toUri().getPath()), ImmutableMap.of("create", "true"))) {
            fileInJar = Files.createFile(jarFileSystem.getPath("/").resolve("file.txt"));
        }
    }

    @Test
    public void by_default_visits_all_files_recursively() throws IOException {
        Retrolambda.visitFiles(inputDir, null, null, visitor);

        assertThat(visitedFiles, containsInAnyOrder(file1, file2, fileInSubdir));
    }

    @Test
    public void when_included_files_is_set_then_visits_only_those_files() throws IOException {
        List<Path> includedFiles = Arrays.asList(file1, fileInSubdir);

        Retrolambda.visitFiles(inputDir, includedFiles, null, visitor);

        assertThat(visitedFiles, containsInAnyOrder(file1, fileInSubdir));
    }

    @Test
    public void ignores_included_files_that_are_outside_the_input_directory() throws IOException {
        List<Path> includedFiles = Arrays.asList(file1, outsider);

        Retrolambda.visitFiles(inputDir, includedFiles, null, visitor);

        assertThat(visitedFiles, containsInAnyOrder(file1));
    }

    @Test
    public void visits_files_in_jar() throws IOException {
        List<Path> jars = Arrays.asList(jar);

        Retrolambda.visitFiles(inputDir, null, jars, visitor);
        List<URI> uris = visitedFiles.stream().map(Path::toUri).collect(Collectors.toList());

        assertThat(uris, containsInAnyOrder(file1.toUri(), file2.toUri(), fileInSubdir.toUri(), fileInJar.toUri()));
    }

    @Test
    public void copies_resources_to_output_directory() throws Throwable {
        Properties p = new Properties();
        p.setProperty(RetrolambdaApi.INPUT_DIR, inputDir.toString());
        p.setProperty(RetrolambdaApi.OUTPUT_DIR, outputDir.toString());
        p.setProperty(RetrolambdaApi.CLASSPATH, "");

        Retrolambda.run(p);

        assertIsFile(outputDir.resolve("file1.txt"));
        assertIsFile(outputDir.resolve("subdir/file.txt"));
    }

    private static void assertIsFile(Path path) {
        assertTrue("Expected " + path + " to be a file", Files.isRegularFile(path));
    }
}
