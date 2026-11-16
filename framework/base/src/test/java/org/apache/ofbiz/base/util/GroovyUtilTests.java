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
     * The import and receiver denylists are statically typed, so dynamically dispatched calls slip past
     * them: {@code 'id'.execute()} runs an OS command through a Groovy extension method on String, and
     * {@code Class.forName(...).getRuntime()...} reaches Runtime without naming it as a receiver type.
     * The expression checker must reject both.
     */
    @Test
    public void parseClassRejectsDynamicExecute() {
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass("'id'.execute()"),
                "GroovyUtil.parseClass() must refuse a script that calls execute() on a String");
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass("['id'].execute()"),
                "GroovyUtil.parseClass() must refuse a script that calls execute() on a List");
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass("Class.forName('java.lang.Runtime')"),
                "GroovyUtil.parseClass() must refuse a script that reaches a class through Class.forName()");
    }

    /**
     * A method name assembled at runtime, or a method pointer, defers the call past the name check, so
     * both must be rejected.
     */
    @Test
    public void parseClassRejectsIndirectMethodDispatch() {
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass("def m = 'exe' + 'cute'; 'id'.\"$m\"()"),
                "GroovyUtil.parseClass() must refuse a script that calls a dynamically-named method");
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass("('id'.&execute)()"),
                "GroovyUtil.parseClass() must refuse a script that calls a method through a method pointer");
    }

    /**
     * Static calls on System, Files or Eval reach the environment, the filesystem or nested evaluation
     * without an instance receiver the receiver denylist could match.
     */
    @Test
    public void parseClassRejectsDangerousStaticCalls() {
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass("System.exit(0)"),
                "GroovyUtil.parseClass() must refuse a script that calls a static method on System");
        String readScript = "java.nio.file.Files.readString(java.nio.file.Path.of('/etc/hosts'))";
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass(readScript),
                "GroovyUtil.parseClass() must refuse a script that reads a file through Files");
    }

    /**
     * An AST-transform annotation such as {@code @ASTTest} runs arbitrary code while the script is
     * compiled, before the SecureASTCustomizer ever inspects it, so any annotation written in the
     * source must be rejected.
     */
    @Test
    public void parseClassRejectsSourceAnnotations() {
        String script = "@groovy.transform.ASTTest(value = { assert true }) def a = 1; a";
        assertThrows(CompilationFailedException.class, () -> GroovyUtil.parseClass(script),
                "GroovyUtil.parseClass() must refuse a script that carries a compile-time annotation");
    }

    /**
     * The restriction must not be so broad that it breaks compiling ordinary, non-malicious scripts --
     * the shape every legitimate internal ${groovy:...} scriptlet and .groovy script location has.
     */
    @Test
    public void parseClassStillAcceptsOrdinaryScripts() {
        assertDoesNotThrow(() -> GroovyUtil.parseClass("1 + 1"),
                "GroovyUtil.parseClass() must keep compiling ordinary, non-malicious scripts");
        assertDoesNotThrow(() -> GroovyUtil.parseClass("def total = [1, 2, 3].collect { it * 2 }.sum(); total > 0 ? total : 0"),
                "GroovyUtil.parseClass() must keep compiling closures, method names and subscripts used by ordinary scripts");
        assertDoesNotThrow(() -> GroovyUtil.parseClass("def m = [a: 1]; m['a']"),
                "GroovyUtil.parseClass() must keep compiling ordinary map subscript access");
    }
}
