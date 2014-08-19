// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.fs.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public class LambdaClassDumper implements AutoCloseable {

    private final Path outputDir;
    private final int targetVersion;
    private Field dumperField;

    public LambdaClassDumper(Path outputDir, int targetVersion) {
        this.outputDir = outputDir;
        this.targetVersion = targetVersion;
    }

    public void install() {
        try {
            Class<?> mf = Class.forName("java.lang.invoke.InnerClassLambdaMetafactory");
            dumperField = mf.getDeclaredField("dumper");
            makeNonFinal(dumperField);
            dumperField.setAccessible(true);

            Path p = new VirtualPath("");
            dumperField.set(null, newProxyClassesDumper(p));
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize dumper", e);
        }
    }

    public void uninstall() {
        if (dumperField != null) {
            try {
                dumperField.set(null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        uninstall();
    }

    private static void makeNonFinal(Field field) throws Exception {
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        int mod = modifiers.getInt(field);
        modifiers.setInt(field, mod & ~Modifier.FINAL);
    }

    private static Object newProxyClassesDumper(Path dumpDir) throws Exception {
        Class<?> dumper = Class.forName("java.lang.invoke.ProxyClassesDumper");
        Constructor<?> c = dumper.getDeclaredConstructor(Path.class);
        c.setAccessible(true);
        return c.newInstance(dumpDir);
    }

    private void reifyLambdaClass(String className, byte[] classfileBuffer) {
        try {
            System.out.println("Saving lambda class: " + className);
            byte[] backportedBytecode = LambdaClassBackporter.transform(classfileBuffer, targetVersion);
            Path savePath = outputDir.resolve(className + ".class");
            Files.createDirectories(savePath.getParent());
            Files.write(savePath, backportedBytecode);

        } catch (Throwable t) {
            // print to stdout to keep in sync with other log output
            System.out.println("ERROR: Failed to backport lambda class: " + className);
            t.printStackTrace(System.out);
        }
    }


    private final class VirtualFSProvider extends FakeFileSystemProvider {

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
            return new ClassChannel(path);
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) {
        }
    }

    private final class VirtualFS extends FakeFileSystem {

        @Override
        public FileSystemProvider provider() {
            return new VirtualFSProvider();
        }
    }

    private final class VirtualPath extends FakePath {

        private final String path;

        public VirtualPath(String path) {
            this.path = path;
        }

        @Override
        public FileSystem getFileSystem() {
            return new VirtualFS();
        }

        @Override
        public Path getParent() {
            return this;
        }

        @Override
        public Path resolve(String other) {
            if (!path.isEmpty()) {
                throw new IllegalStateException();
            }
            return new VirtualPath(other);
        }

        @Override
        public String toString() {
            return path;
        }
    }

    private final class ClassChannel extends FakeSeekableByteChannel {
        private final Path path;
        private final ByteArrayOutputStream os;
        private final WritableByteChannel ch;

        public ClassChannel(Path path) {
            this.path = path;
            this.os = new ByteArrayOutputStream();
            this.ch = Channels.newChannel(os);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return ch.write(src);
        }

        @Override
        public void close() {
            String className = path.toString();
            className = className.substring(0, className.length() - 6);
            if (LambdaReifier.isLambdaClassToReify(className)) {
                reifyLambdaClass(className, os.toByteArray());
            }
        }
    }
}
