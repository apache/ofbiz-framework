/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.base.container;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class path object.
 *
 * <p>This reifies the notion of a Java class path to be able to manipulate them programmatically.
 */
final class Classpath {
    /** {@code .jar} and {@code .zip} files matcher. */
    private static final PathMatcher JAR_ZIP_FILES = FileSystems.getDefault().getPathMatcher("glob:*.{java,zip}");

    /** A sequence of unique path elements. */
    private final LinkedHashSet<Path> elements = new LinkedHashSet<>();

    /**
     * Adds a directory or a file to the class path.
     *
     * In the directory case, all files ending with ".jar" or ".zip" inside this directory
     * are added to the class path.
     *
     * @param file  the absolute normalized file name of a directory or a file that must exist
     * @param type  either "dir" or "jar"
     * @throws NullPointerException when {@code file} is {@code null}.
     */
    void add(Path file, String type) {
        elements.add(file);
        if (Files.isDirectory(file) && "dir".equals(type)) {
            try (Stream<Path> innerFiles = Files.list(file)) {
                innerFiles.filter(JAR_ZIP_FILES::matches).forEach(elements::add);
            } catch (IOException e) {
                String fmt = "Warning : Module classpath component '%s' is not valid and will be ignored...";
                System.err.println(String.format(fmt, file));
            }
        }
    }

    @Override
    public String toString() {
        return elements.stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
    }

    /**
     * Returns the list of class path component URIs.
     *
     * @return a list of class path component URIs
     */
    List<URI> toUris() {
        return elements.stream()
                .map(Path::toUri)
                .collect(Collectors.toList());
     }
}
