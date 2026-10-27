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
 *******************************************************************************/
package org.apache.ofbiz.widget.renderer.macro;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guards the rule that the macro renderers pass the values to the macros as data and never turn them into template
 * text: the only template text that is compiled is the constant text of {@link FtlWriter} that calls a macro.
 */
public final class MacroRenderersTemplateTextGuardTest {

    private static final String MACRO_PACKAGE_PATH = "org/apache/ofbiz/widget/renderer/macro";
    private static final String FTL_WRITER = "FtlWriter.java";

    private static Path macroSources() {
        for (final String root : new String[] {"framework/widget/src/main/java/", "src/main/java/"}) {
            final Path path = Paths.get(root + MACRO_PACKAGE_PATH);
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return null;
    }

    private static List<Path> javaFiles(final Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(file -> file.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static String read(final Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test
    public void onlyFtlWriterBuildsMacroCallsOrCompilesTemplates() throws IOException {
        final Path sources = macroSources();
        assumeTrue(sources != null, "the sources of the macro renderers are not available from the working directory");

        final List<String> offenders = new ArrayList<>();
        for (final Path file : javaFiles(sources)) {
            final String name = file.getFileName().toString();
            if (FTL_WRITER.equals(name)) {
                continue;
            }
            final String content = read(file);
            if (content.contains("\"<@")) {
                offenders.add(name + " builds a macro call as text");
            }
            if (content.contains("new Template(")) {
                offenders.add(name + " compiles a template");
            }
        }
        assertThat(offenders, empty());
    }

    @Test
    public void ftlWriterCompilesOnlyTheConstantMacroCallTemplate() throws IOException {
        final Path sources = macroSources();
        assumeTrue(sources != null, "the sources of the macro renderers are not available from the working directory");

        final String content = read(sources.resolve(FTL_WRITER));
        assertThat("FtlWriter compiles a single template", content.split("new Template\\(", -1).length - 1, equalTo(1));
        assertThat(content.contains("MACRO_CALL_TEMPLATE"), equalTo(true));
    }
}
