// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.api;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.*;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Set of mappings from new API classes or methods (like {@link Double#isFinite(double)} or
 * {@link java.util.function.Predicate} to backported classes in third-party libraries, like
 * {@link com.google.common.primitives.Doubles#isFinite(double)} or {@code java8.util.function.Predicate} in
 * the streamsupport library.
 */
public class ApiMappingSet {

    /**
     * Mapping from unsupported API package to new package, using the internal
     * format. For example, "java/util/function/Predicate" to "java8/util/function/Predicate"
     */
    private Map<String, String> packageMappings = new HashMap<>();

    /**
     * Mapping from unsupported API class to a new class, using the internal name format.
     * For example, from "java/nio/StandardCharsets" to "com/google/common/base/Charsets"
     */
    private Map<String, String> classNameMappings = new HashMap<>();

    /**
     * Mapping from a new static field to a static field of the same type defined elsewhere.
     *
     * For example, from "java/nio/StandardCharsets.UTF_8" to "com/google/common/base/Charsets.UTF_8"
     */
    private Map<String, Mapping> fieldMappings = new HashMap<>();

    /**
     * Mapping from a single static method to a different static method with the same signature.
     *
     * For example, from "java/lang/Double.isFinite" to "com/google/common/primitives/Doubles.isFinite"
     */
    private Map<String, Mapping> staticMethodMappings = new HashMap<>();

    public ApiMappingSet(List<String> mappings) throws IOException {
        for (String mapping : mappings) {
            read(mapping);
        }
    }

    /**
     * @return {@code true} if any mappings are defined
     */
    public boolean isEnabled() {
        return !classNameMappings.isEmpty() ||
                !fieldMappings.isEmpty() ||
                !staticMethodMappings.isEmpty() ||
                !packageMappings.isEmpty();
    }

    private void read(String mapping) throws IOException {

        CharSource charSource = tryReadResource(mapping).orElseGet(() -> readFile(mapping));

        parse(charSource);
    }

    private Optional<CharSource> tryReadResource(String mapping) {
        URL url;
        try {
            url = Resources.getResource(ApiMappingSet.class, mapping);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        return Optional.of(Resources.asByteSource(url).asCharSource(Charsets.UTF_8));
    }

    private CharSource readFile(String path) {
        File file = new File(path);
        return Files.asCharSource(file, Charsets.UTF_8);
    }

    private void parse(CharSource charSource) throws IOException {

        ImmutableList<String> lines = charSource.readLines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Strip comments
            int commentStart = line.indexOf('#');
            if(commentStart != -1) {
                line = line.substring(0, commentStart);
            }

            // Skip blank lines
            if(line.trim().isEmpty()) {
                continue;
            }

            String[] columns = line.split("\\s+");

            if(columns.length != 3) {
                throw new InvalidApiMappingSyntax(i, "Expected a line with three columns, separated with whitespace");
            }


            ApiMappingType type;
            try {
                type = ApiMappingType.valueOf(columns[0].toUpperCase());
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                throw new InvalidApiMappingSyntax(i, "Expected a line start with one of: " +
                        Arrays.toString(ApiMappingType.values()));
            }

            switch (type) {

                case PACKAGE:
                    packageMappings.put(columns[1], columns[2]);
                    break;

                case CLASS:
                    classNameMappings.put(columns[1], columns[2]);
                    break;

                case INVOKESTATIC:
                    staticMethodMappings.put(columns[1], new Mapping(columns[2]));
                    break;

                case GETSTATIC:
                    fieldMappings.put(columns[1], new Mapping(columns[2]));
                    break;
            }
        }
    }

    /**
     * Maps an internal class name to the backported class name.
     */
    public String mapClass(String internalTypeName) {

        // Try to match by exact class name
        String className = classNameMappings.get(internalTypeName);
        if(className != null) {
            return className;
        }

        // Try to match by package
        int packageStart = internalTypeName.lastIndexOf('/');
        while(packageStart != -1) {
            String prefix = internalTypeName.substring(0, packageStart);
            String suffix = internalTypeName.substring(packageStart);
            if(packageMappings.containsKey(prefix)) {
                return packageMappings.get(prefix) + suffix;
            }
            packageStart = internalTypeName.lastIndexOf('/', packageStart - 1);
        }

        // No mappings, use the original
        return internalTypeName;
    }

    public Mapping mapField(String owner, String name, String descriptor) {
        return mapMember(fieldMappings, owner, name, descriptor);
    }

    public Mapping mapStaticMethod(String owner, String name, String descriptor) {
        return mapMember(staticMethodMappings, owner, name, descriptor);
    }

    private Mapping mapMember(Map<String, Mapping> dictionary, String owner, String name, String descriptor) {
        Mapping mapping = dictionary.get(owner + "." + name + descriptor);
        if(mapping != null) {
            return mapping;
        }

        return new Mapping(mapClass(owner), name);
    }
}