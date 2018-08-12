// Copyright © 2016 Panayotis Katsaloulis <www.panayotis.com>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package net.orfjackal.retrolambda.interfaces;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LibraryInterfaces {

    private final Collection<String> requiredInterfaces = new HashSet<>();
    private final Collection<String> inputDirInterfaces = new HashSet<>();
    private final Collection<String> resolvedInterfaces = new HashSet<>();

    public void addRequiredInterfaces(String[] interfaceNames) {
        requiredInterfaces.addAll(Arrays.asList(interfaceNames));
    }

    public void addFoundInterface(String interfaceName) {
        inputDirInterfaces.add(interfaceName);
    }

    public Collection<byte[]> getMissingInterfaces(List<Path> classpath) {
        Collection<String> missingInterfaces = new HashSet<>(requiredInterfaces);
        missingInterfaces.removeAll(inputDirInterfaces);
        Collection<String> justFound = new HashSet<>();
        Collection<byte[]> result = new HashSet<>();

        for (Path path : classpath) {
            if (Files.isDirectory(path))
                for (String missingName : missingInterfaces) {
                    Path possibleTarget = path.resolve(missingName + ".class");
                    if (Files.isRegularFile(possibleTarget))
                        try {
                            result.add(Files.readAllBytes(possibleTarget));
                            justFound.add(missingName);
                        } catch (IOException ex) {
                        }
                }
            else if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".jar")) {
                JarFile jar = null;
                try {
                    jar = new JarFile(path.toFile());
                } catch (IOException ex) {
                }
                if (jar != null)
                    for (String missingName : missingInterfaces) {
                        JarEntry entry = jar.getJarEntry(missingName + ".class");
                        if (entry != null)
                            try (InputStream inputStream = jar.getInputStream(entry)) {
                                byte[] bytes = getBytes(inputStream);
                                if (bytes != null)
                                    result.add(bytes);
                                justFound.add(missingName);
                            } catch (IOException ex) {
                            }
                    }
            }
            missingInterfaces.removeAll(justFound);
            resolvedInterfaces.addAll(justFound);
            justFound.clear();
        }
        return result;
    }

    public Predicate<String> getAcceptedPredicate() {
        return className -> {
            if (className.endsWith("$"))
                className = className.substring(0, className.length() - 1);
            /**
             * Default implementation classes will be emitted only when this
             * class appears in the current classes location, not inside the
             * library classpath. It is the library responsibility to provide
             * default implementation classes for itself. Thus the
             * implementation classes will be emitted only once, when the
             * library is (possibly) downgraded, and not every time the library
             * is consumed by any other project.
             */
            return !resolvedInterfaces.contains(className);
        };
    }

    private byte[] getBytes(InputStream in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(100);
        int value;
        try {
            while ((value = in.read()) >= 0)
                out.write(value);
        } catch (IOException ex) {
            return null;
        }
        return out.toByteArray();
    }

}
