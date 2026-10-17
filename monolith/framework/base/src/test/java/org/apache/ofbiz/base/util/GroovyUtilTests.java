/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ofbiz.base.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.jupiter.api.Test;

public class GroovyUtilTests {

    /**
     * GroovyUtil.parseClass() backs ScriptUtil.parseScript(), which is what FlexibleStringExpander uses to
     * compile every {@code ${groovy:...}} substring it finds -- including ones that originate from a
     * caller-supplied string reaching FlexibleStringExpander.expandString() before any application-level
     * fix strips that call out. Before the fix, parseClass() used a GroovyClassLoader with no
     * SecureASTCustomizer, so this construction compiled cleanly. ProcessBuilder is java.lang, so no
     * import statement is needed to reach it -- an import denylist alone would not catch this.
     */
    @Test
    public void parseClassRejectsProcessBuilderConstruction() {
        String maliciousScript = "new ProcessBuilder(['id']).start()";
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass(maliciousScript),
                "GroovyUtil.parseClass() must refuse to compile a script that constructs a ProcessBuilder");
    }

    /**
     * Same restriction, reached via a method call on an existing receiver rather than a constructor call.
     */
    @Test
    public void parseClassRejectsRuntimeExec() {
        String maliciousScript = "Runtime.getRuntime().exec('id')";
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass(maliciousScript),
                "GroovyUtil.parseClass() must refuse to compile a script that calls Runtime.exec()");
    }

    /**
     * The restriction must not be so broad that it breaks compiling ordinary, non-malicious scripts --
     * the shape every legitimate internal ${groovy:...} scriptlet and .groovy script location has.
     */
    @Test
    public void parseClassStillAcceptsOrdinaryScripts() {
        assertDoesNotThrow(() -> GroovyUtil.parseClass("1 + 1"),
                "GroovyUtil.parseClass() must keep compiling ordinary, non-malicious scripts");
    }
}
