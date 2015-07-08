// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
    }

    @Test
    public void by_default_visits_all_files_recursively() throws IOException {
        Retrolambda.visitFiles(inputDir, null, visitor);

        assertThat(visitedFiles, containsInAnyOrder(file1, file2, fileInSubdir));
    }

    @Test
    public void when_included_files_is_set_then_visits_only_those_files() throws IOException {
        List<Path> includedFiles = Arrays.asList(file1, fileInSubdir);

        Retrolambda.visitFiles(inputDir, includedFiles, visitor);

        assertThat(visitedFiles, containsInAnyOrder(file1, fileInSubdir));
    }

    @Test
    public void ignores_included_files_that_are_outside_the_input_directory() throws IOException {
        List<Path> includedFiles = Arrays.asList(file1, outsider);

        Retrolambda.visitFiles(inputDir, includedFiles, visitor);

        assertThat(visitedFiles, containsInAnyOrder(file1));
    }

    @Test
    public void copies_resources_to_output_directory() throws Throwable {
        Properties p = new Properties();
        p.setProperty(Config.INPUT_DIR, inputDir.toString());
        p.setProperty(Config.OUTPUT_DIR, outputDir.toString());
        p.setProperty(Config.CLASSPATH, "");

        Retrolambda.run(new Config(p));

        assertIsFile(outputDir.resolve("file1.txt"));
        assertIsFile(outputDir.resolve("subdir/file.txt"));
    }

    private static void assertIsFile(Path path) {
        assertTrue("Expected " + path + " to be a file", Files.isRegularFile(path));
    }
}
