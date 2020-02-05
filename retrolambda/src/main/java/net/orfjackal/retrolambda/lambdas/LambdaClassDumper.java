// Copyright © 2013-2020 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

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

    private final LambdaClassSaver lambdaClassSaver;
    private Field dumperField;

    public LambdaClassDumper(LambdaClassSaver lambdaClassSaver) {
        this.lambdaClassSaver = lambdaClassSaver;
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
            throw new IllegalStateException("Cannot initialize dumper; unexpected JDK implementation. " +
                    "Please run Retrolambda using the Java agent (enable forking in the Maven plugin).", e);
        }
    }

    public void uninstall() {
        if (dumperField != null) {
            try {
                dumperField.set(null, null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        uninstall();
    }

    private static void makeNonFinal(Field field) throws Exception {
        try {
            Field modifiers = field.getClass().getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            int mod = modifiers.getInt(field);
            modifiers.setInt(field, mod & ~Modifier.FINAL);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to make a field non-final (" + field + "). " +
                    "This known to fail on Java 12 and newer. Prefer using Java 8 or try using the Java agent " +
                    "(fork=true in the Maven plugin).", e);
        }
    }

    private static Object newProxyClassesDumper(Path dumpDir) throws Exception {
        Class<?> dumper = Class.forName("java.lang.invoke.ProxyClassesDumper");
        Constructor<?> c = dumper.getDeclaredConstructor(Path.class);
        c.setAccessible(true);
        return c.newInstance(dumpDir);
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
            className = className.substring(0, className.lastIndexOf(".class"));
            lambdaClassSaver.saveIfLambda(className, os.toByteArray());
        }
    }
}
