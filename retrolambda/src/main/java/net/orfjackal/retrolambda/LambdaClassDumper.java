// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.*;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

public class LambdaClassDumper {

    private final Path outputDir;
    private final int targetVersion;
    private Field dumperField;

    public LambdaClassDumper(Path outputDir, int targetVersion) {
        this.outputDir = outputDir;
        this.targetVersion = targetVersion;
    }

    public void registerDumper() {
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

    public void unregisterDumper() {
        if (dumperField != null) {
            try {
                dumperField.set(null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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


    private final class VirtualProvider extends FileSystemProvider {

        @Override
        public String getScheme() {
            throw new IllegalStateException();
        }

        @Override
        public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
            throw new IllegalStateException();
        }

        @Override
        public FileSystem getFileSystem(URI uri) {
            throw new IllegalStateException();
        }

        @Override
        public Path getPath(URI uri) {
            throw new IllegalStateException();
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
            return new ClassChannel(path);
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            throw new IllegalStateException();
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) {
        }

        @Override
        public void delete(Path path) {
            throw new IllegalStateException();
        }

        @Override
        public void copy(Path source, Path target, CopyOption... options) {
            throw new IllegalStateException();
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) {
            throw new IllegalStateException();
        }

        @Override
        public boolean isSameFile(Path path, Path path2) {
            throw new IllegalStateException();
        }

        @Override
        public boolean isHidden(Path path) {
            throw new IllegalStateException();
        }

        @Override
        public FileStore getFileStore(Path path) {
            throw new IllegalStateException();
        }

        @Override
        public void checkAccess(Path path, AccessMode... modes) {
            throw new IllegalStateException();
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
            throw new IllegalStateException();
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) {
            throw new IllegalStateException();
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
            throw new IllegalStateException();
        }

        @Override
        public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
            throw new IllegalStateException();
        }
    }

    private final class VirtualFS extends FileSystem {

        @Override
        public FileSystemProvider provider() {
            return new VirtualProvider();
        }

        @Override
        public void close() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isOpen() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isReadOnly() {
            throw new IllegalStateException();
        }

        @Override
        public String getSeparator() {
            throw new IllegalStateException();
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            throw new IllegalStateException();
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            throw new IllegalStateException();
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            throw new IllegalStateException();
        }

        @Override
        public Path getPath(String first, String... more) {
            throw new IllegalStateException();
        }

        @Override
        public PathMatcher getPathMatcher(String syntaxAndPattern) {
            throw new IllegalStateException();
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            throw new IllegalStateException();
        }

        @Override
        public WatchService newWatchService() {
            throw new IllegalStateException();
        }
    }

    private final class VirtualPath implements Path {

        private final String path;

        public VirtualPath(String path) {
            this.path = path;
        }

        @Override
        public FileSystem getFileSystem() {
            return new VirtualFS();
        }

        @Override
        public boolean isAbsolute() {
            throw new IllegalStateException();
        }

        @Override
        public Path getRoot() {
            throw new IllegalStateException();
        }

        @Override
        public Path getFileName() {
            throw new IllegalStateException();
        }

        @Override
        public Path getParent() {
            return this;
        }

        @Override
        public int getNameCount() {
            throw new IllegalStateException();
        }

        @Override
        public Path getName(int index) {
            throw new IllegalStateException();
        }

        @Override
        public Path subpath(int beginIndex, int endIndex) {
            throw new IllegalStateException();
        }

        @Override
        public boolean startsWith(Path other) {
            throw new IllegalStateException();
        }

        @Override
        public boolean startsWith(String other) {
            throw new IllegalStateException();
        }

        @Override
        public boolean endsWith(Path other) {
            throw new IllegalStateException();
        }

        @Override
        public boolean endsWith(String other) {
            throw new IllegalStateException();
        }

        @Override
        public Path normalize() {
            throw new IllegalStateException();
        }

        @Override
        public Path resolve(Path other) {
            throw new IllegalStateException();
        }

        @Override
        public Path resolve(String other) {
            assert path.isEmpty();
            return new VirtualPath(other);
        }

        @Override
        public Path resolveSibling(Path other) {
            throw new IllegalStateException();
        }

        @Override
        public Path resolveSibling(String other) {
            throw new IllegalStateException();
        }

        @Override
        public Path relativize(Path other) {
            throw new IllegalStateException();
        }

        @Override
        public URI toUri() {
            throw new IllegalStateException();
        }

        @Override
        public Path toAbsolutePath() {
            throw new IllegalStateException();
        }

        @Override
        public Path toRealPath(LinkOption... options) {
            throw new IllegalStateException();
        }

        @Override
        public File toFile() {
            throw new IllegalStateException();
        }

        @Override
        public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
            throw new IllegalStateException();
        }

        @Override
        public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) {
            throw new IllegalStateException();
        }

        @Override
        public Iterator<Path> iterator() {
            throw new IllegalStateException();
        }

        @Override
        public int compareTo(Path other) {
            throw new IllegalStateException();
        }

        @Override
        public String toString() {
            return path;
        }
    }

    private final class ClassChannel implements SeekableByteChannel {
        private final Path path;
        private final ByteArrayOutputStream os;
        private final WritableByteChannel ch;

        public ClassChannel(Path path) {
            this.path = path;
            this.os = new ByteArrayOutputStream();
            this.ch = Channels.newChannel(os);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            throw new IOException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return ch.write(src);
        }

        @Override
        public long position() throws IOException {
            throw new IOException();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            throw new IOException();
        }

        @Override
        public long size() throws IOException {
            throw new IOException();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new IOException();
        }

        @Override
        public boolean isOpen() {
            return true;
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
