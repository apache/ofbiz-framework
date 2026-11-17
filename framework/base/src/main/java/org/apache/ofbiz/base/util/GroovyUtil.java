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
package org.apache.ofbiz.base.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Groovy Utilities.
 *
 */
public final class GroovyUtil {

    private static final String MODULE = GroovyUtil.class.getName();
    // Method names that reach OS execution, dynamic class loading, reflection, nested script
    // evaluation or file / URL reads. Rejected as method-call and static-call targets in
    // sandboxed scripts.
    private static final List<String> DISALLOWED_METHOD_NAMES = List.of(
            "execute", "exec", "start", "forName", "evaluate", "invokeMethod", "getMetaClass",
            "setMetaClass", "getClassLoader", "getRuntime", "newInstance", "loadClass",
            "getDeclaredMethod", "getMethod", "parseClass", "run",
            "toURL", "toURI", "getText", "readLines", "eachLine", "newReader", "newInputStream",
            "withReader", "withInputStream", "readBytes");
    // Properties that hand back the runtime, the class or a class loader to call the above on, or
    // that read a file / URL body (the GDK text and bytes properties).
    private static final List<String> DISALLOWED_PROPERTY_NAMES = List.of(
            "class", "metaClass", "classLoader", "runtime", "text", "bytes");
    // Types whose constructors give OS execution, dynamic evaluation or filesystem / network access.
    private static final List<String> DISALLOWED_CONSTRUCTOR_TYPES = List.of(
            "java.lang.ProcessBuilder", "java.lang.Thread", "java.io.File", "java.net.URL",
            "java.net.Socket", "groovy.lang.GroovyShell", "groovy.lang.GroovyClassLoader");
    // Classes whose static methods reach OS execution, the filesystem or nested evaluation; the
    // SecureASTCustomizer receiver list only blocks instance receivers, not these static targets.
    private static final List<String> DISALLOWED_STATIC_TARGETS = List.of(
            "java.lang.System", "java.nio.file.Files", "groovy.util.Eval", "groovy.lang.Eval");
    // Types a coercion ("'/etc/passwd' as File") can turn a String into to reach the filesystem or
    // network. A cast bypasses the constructor and method-name checks, so the cast target is
    // checked too.
    private static final List<String> DISALLOWED_CAST_TYPES = List.of(
            "java.io.File", "java.net.URL", "java.net.URI", "java.nio.file.Path",
            "java.io.InputStream", "java.io.Reader");
    private static final UtilCache<String, Class<?>> PARSED_SCRIPTS = UtilCache.createUtilCache("script.GroovyLocationParsedCache", 0, 0, false);
    private static final GroovyClassLoader GROOVY_CLASS_LOADER;
    private static final GroovyClassLoader GROOVY_CLASS_LOADER_FOR_LOCATION;
    private static final CompilerConfiguration SANDBOXED_COMPILER_CONFIG;

    private GroovyUtil() { }

    static {
        GroovyClassLoader sandboxedClassLoader = null;
        GroovyClassLoader locationClassLoader = null;
        String scriptBaseClass = UtilProperties.getPropertyValue("groovy", "scriptBaseClass");
        if (!scriptBaseClass.isEmpty()) {
            // Used by parseClass(String): compiles ${groovy:...} substrings and inline <script> bodies passed to
            // ScriptUtil.parseScript(), which can carry caller-supplied text. Keep the compile-time
            // SecureASTCustomizer here as an injection backstop (see GroovyUtilTests).
            CompilerConfiguration sandboxedConf = new CompilerConfiguration();
            sandboxedConf.setScriptBaseClass(scriptBaseClass);
            sandboxedConf.addCompilationCustomizers(new RejectSourceAnnotationsCustomizer(), buildSecureAstCustomizer());
            sandboxedClassLoader = new GroovyClassLoader(GroovyUtil.class.getClassLoader(), sandboxedConf);

            // Used by parseClass(InputStream, location): compiles *.groovy files resolved from a component://
            // location (controller events, service implementations, screen scripts). These are first-party
            // files shipped with the framework/plugins, never request input, so they are intentionally NOT
            // run through SecureASTCustomizer - some legitimately use java.lang.Thread / java.lang.Runtime
            // (e.g. the webtools Threads and Cache Maintenance screens) and must still compile.
            CompilerConfiguration locationConf = new CompilerConfiguration();
            locationConf.setScriptBaseClass(scriptBaseClass);
            locationClassLoader = new GroovyClassLoader(GroovyUtil.class.getClassLoader(), locationConf);
        }
        GROOVY_CLASS_LOADER = sandboxedClassLoader;
        GROOVY_CLASS_LOADER_FOR_LOCATION = locationClassLoader;
    }

    static {
        SANDBOXED_COMPILER_CONFIG = new CompilerConfiguration();
        SANDBOXED_COMPILER_CONFIG.addCompilationCustomizers(new RejectSourceAnnotationsCustomizer(), buildSecureAstCustomizer());
    }

    /**
     * Builds a fresh {@link SecureASTCustomizer} applying the compile-time AST restrictions used by
     * GROOVY_CLASS_LOADER (parseClass(String)) and SANDBOXED_COMPILER_CONFIG (eval()), but not by
     * GROOVY_CLASS_LOADER_FOR_LOCATION (trusted component:// *.groovy files). Blocks OS-execution APIs and dynamic class-loading
     * as a defence-in-depth measure.
     * <p>Returns a new instance on every call rather than a shared constant: {@code SecureASTCustomizer}
     * visits the AST during compilation, so handing the same instance to two {@code CompilerConfiguration}s
     * used concurrently would be unsafe.
     * <p>Note: SecureASTCustomizer operates at compile time and does not constitute a complete sandbox;
     * expressions compiled through either path should never originate from untrusted input. An
     * expression checker is added to also reject the common dynamic-dispatch routes (dynamic method
     * or property names, blocked method / property names, method pointers, dangerous constructors
     * and static calls) that the statically-typed import and receiver lists cannot catch.
     * @return a new, independently-usable SecureASTCustomizer
     */
    private static SecureASTCustomizer buildSecureAstCustomizer() {
        SecureASTCustomizer secureAst = new SecureASTCustomizer();
        secureAst.setDisallowedImports(List.of(
                "java.lang.Runtime",
                "java.lang.ProcessBuilder",
                "java.lang.ClassLoader",
                "java.lang.Thread",
                "java.lang.reflect.Method",
                "java.lang.reflect.Field",
                "java.net.Socket",
                "java.net.ServerSocket",
                "groovy.lang.GroovyShell",
                "groovy.lang.GroovyClassLoader"));
        @SuppressWarnings("rawtypes")
        List<Class> blockedReceivers = Arrays.asList(
                Runtime.class,
                ProcessBuilder.class,
                Thread.class,
                ClassLoader.class);
        secureAst.setDisallowedReceiversClasses(blockedReceivers);
        // The import and receiver lists above only catch statically-typed references. Groovy
        // expressions are dynamically typed, so a call like 'id'.execute() or Class.forName(...)
        // is not caught by them. This expression checker closes the common dynamic-dispatch routes:
        // blocked method and property names (including the file / URL read helpers and the text /
        // bytes properties), dynamically-computed method or property names, method pointers,
        // dangerous constructors, coercions to File / URL / Path, and static calls on
        // System / Files / Eval.
        secureAst.addExpressionCheckers(GroovyUtil::isExpressionAllowed);
        return secureAst;
    }

    /**
     * Rejects the dynamic-dispatch routes that reach OS execution, dynamic class loading or nested
     * evaluation but slip past the statically-typed import and receiver lists of a
     * {@link SecureASTCustomizer}. This is a denylist and a defence-in-depth measure, not a complete
     * sandbox; expressions must never originate from untrusted input.
     * @param expression the expression being compiled
     * @return {@code false} to reject the expression, {@code true} to allow it
     */
    private static boolean isExpressionAllowed(Expression expression) {
        if (expression instanceof MethodCallExpression) {
            MethodCallExpression call = (MethodCallExpression) expression;
            if (call.getObjectExpression() instanceof ClassExpression
                    && DISALLOWED_STATIC_TARGETS.contains(((ClassExpression) call.getObjectExpression()).getType().getName())) {
                return false;
            }
            // A method name built at runtime (e.g. 'id'."${m}"()) cannot be checked, so reject it.
            if (!(call.getMethod() instanceof ConstantExpression)) {
                return false;
            }
            return !DISALLOWED_METHOD_NAMES.contains(call.getMethodAsString());
        }
        if (expression instanceof StaticMethodCallExpression) {
            return !DISALLOWED_METHOD_NAMES.contains(((StaticMethodCallExpression) expression).getMethod());
        }
        if (expression instanceof MethodPointerExpression) {
            // A method pointer (e.g. 'id'.&execute) defers the call past this check, so reject it.
            return false;
        }
        if (expression instanceof ConstructorCallExpression) {
            return !DISALLOWED_CONSTRUCTOR_TYPES.contains(((ConstructorCallExpression) expression).getType().getName());
        }
        if (expression instanceof PropertyExpression) {
            PropertyExpression property = (PropertyExpression) expression;
            if (!(property.getProperty() instanceof ConstantExpression)) {
                return false;
            }
            return !DISALLOWED_PROPERTY_NAMES.contains(property.getPropertyAsString());
        }
        if (expression instanceof ClassExpression) {
            String type = ((ClassExpression) expression).getType().getName();
            return !("java.lang.Class".equals(type) || "java.lang.Runtime".equals(type)
                    || "java.lang.ProcessBuilder".equals(type));
        }
        if (expression instanceof CastExpression) {
            // A coercion such as ('/etc/passwd' as File) reaches the filesystem or network without a
            // constructor call or a blocked method name, so the cast target type is checked here.
            return !DISALLOWED_CAST_TYPES.contains(((CastExpression) expression).getType().getName());
        }
        return true;
    }

    /**
     * A {@link CompilationCustomizer} that rejects any annotation written in the compiled source.
     * AST-transform annotations such as {@code @ASTTest} and {@code @Grab} run arbitrary code at
     * compile time, before a {@link SecureASTCustomizer} ever sees the result, so a sandboxed
     * script must not be allowed to carry them. Synthetic annotations that Groovy adds itself
     * (for example {@code @Generated}) have no source position and are left alone.
     */
    private static final class RejectSourceAnnotationsCustomizer extends CompilationCustomizer {

        RejectSourceAnnotationsCustomizer() {
            super(CompilePhase.CONVERSION);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            rejectSourceAnnotations(classNode);
            ClassCodeVisitorSupport visitor = new ClassCodeVisitorSupport() {
                @Override
                protected SourceUnit getSourceUnit() {
                    return source;
                }

                @Override
                public void visitAnnotations(AnnotatedNode node) {
                    rejectSourceAnnotations(node);
                    super.visitAnnotations(node);
                }
            };
            visitor.visitClass(classNode);
        }

        private static void rejectSourceAnnotations(AnnotatedNode node) {
            for (AnnotationNode annotation : node.getAnnotations()) {
                if (annotation.getLineNumber() > 0) {
                    throw new SecurityException("Annotations are not allowed in a sandboxed Groovy script: @"
                            + annotation.getClassNode().getName());
                }
            }
        }
    }

    /**
     * Evaluate a Groovy condition or expression.
     * <p>The shell is created with a restricted {@link CompilerConfiguration} backed by
     * {@link SecureASTCustomizer}: explicit imports of OS-execution and dynamic class-loading
     * APIs ({@code Runtime}, {@code ProcessBuilder}, {@code Thread}, {@code ClassLoader} and
     * related reflection / network classes) are disallowed, and those same types are blocked
     * as method-call receivers.  This is a compile-time, defence-in-depth measure; it does
     * not constitute a complete sandbox, and expressions must never originate from untrusted
     * user input.
     * @param expression The expression to evaluate
     * @param context The context to use in evaluation (re-written)
     * @see <a href="StringUtil.html#convertOperatorSubstitutions(java.lang.String)">StringUtil.convertOperatorSubstitutions(java.lang.String)</a>
     * @return Object The result of the evaluation
     * @throws CompilationFailedException
     */
    @SuppressWarnings("unchecked")
    public static Object eval(String expression, Map<String, Object> context) throws CompilationFailedException {
        Object o;
        if (expression == null || "".equals(expression)) {
            Debug.logError("Groovy Evaluation error. Empty expression", MODULE);
            return null;
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Evaluating -- " + expression, MODULE);
            Debug.logVerbose("Using Context -- " + context, MODULE);
        }
        try {
            GroovyShell shell = new GroovyShell(GroovyUtil.class.getClassLoader(), getBinding(context, expression), SANDBOXED_COMPILER_CONFIG);
            o = shell.evaluate(StringUtil.convertOperatorSubstitutions(expression));
            if (Debug.verboseOn()) {
                Debug.logVerbose("Evaluated to -- " + o, MODULE);
            }
            // read back the context info
            Binding binding = shell.getContext();
            context.putAll(binding.getVariables());
        } catch (CompilationFailedException e) {
            Debug.logError(e, "Groovy Evaluation error.", MODULE);
            throw e;
        }
        return o;
    }

    /** Returns a <code>Binding</code> instance initialized with the
     * variables contained in <code>context</code>. If <code>context</code>
     * is <code>null</code>, an empty <code>Binding</code> is returned.
     * <p>The expression is parsed to initiate non existing variable
     * in <code>Binding</code> to null for GroovyShell evaluation.
     * <p>The <code>context Map</code> is added to the <code>Binding</code>
     * as a variable called "context" so that variables can be passed
     * back to the caller. Any variables that are created in the script
     * are lost when the script ends unless they are copied to the
     * "context" <code>Map</code>.</p>
     * @param context A <code>Map</code> containing initial variables
     * @return A <code>Binding</code> instance
     */
    private static Binding getBinding(Map<String, Object> context, String expression) {
        Map<String, Object> vars = new HashMap<>();
        if (context != null) {
            vars.putAll(context);
            if (UtilValidate.isNotEmpty(expression)) {
                //analyse expression to find variables by split non alpha, ignoring "_" to allow my_variable usage
                String[] variables = expression.split("[\\P{Alpha}&&[^_]]+");
                for (String variable: variables) {
                    if (!vars.containsKey(variable)) {
                        vars.put(variable, null);
                    }
                }
            }
            vars.put("context", context);
            if (vars.get(ScriptUtil.SCRIPT_HELPER_KEY) == null) {
                ScriptContext scriptContext = ScriptUtil.createScriptContext(context);
                ScriptHelper scriptHelper = (ScriptHelper) scriptContext.getAttribute(ScriptUtil.SCRIPT_HELPER_KEY);
                if (scriptHelper != null) {
                    vars.put(ScriptUtil.SCRIPT_HELPER_KEY, scriptHelper);
                }
            }
        }
        return new Binding(vars);
    }

    public static Binding getBinding(Map<String, Object> context) {
        return getBinding(context, null);
    }

    public static Class<?> getScriptClassFromLocation(String location) throws GeneralException {
        try {
            Class<?> scriptClass = PARSED_SCRIPTS.get(location);
            if (scriptClass == null) {
                URL scriptUrl = FlexibleLocation.resolveLocation(location);
                if (scriptUrl == null || UtilValidate.isUrlInStringAndDoesNotStartByComponentProtocol(scriptUrl.toString())) {
                    throw new GeneralException("Script not found at location [" + location + "]");
                }
                scriptClass = parseClass(scriptUrl.openStream(), location);
                Class<?> scriptClassCached = PARSED_SCRIPTS.putIfAbsent(location, scriptClass);
                if (scriptClassCached == null) { // putIfAbsent returns null if the class is added to the cache
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Cached Groovy script at: " + location, MODULE);
                    }
                } else {
                    // the newly parsed script is discarded and the one found in the cache (that has been created by a concurrent thread in the
                    // meantime) is used
                    scriptClass = scriptClassCached;
                }
            }
            return scriptClass;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException("Error loading Groovy script at [" + location + "]: ", e);
        }
    }

    /**
     * Parses a Groovy class from an input stream.
     * <p>
     * This method is useful for parsing a Groovy script referenced by
     * a flexible location like {@code component://myComponent/script.groovy}.
     * @param in  the input stream containing the class source code
     * @param location  the file name to associate with this class
     * @return the corresponding class object
     * @throws IOException when parsing fails
     */
    private static Class<?> parseClass(InputStream in, String location) throws IOException {
        String classText = UtilIO.readString(in);
        if (GROOVY_CLASS_LOADER_FOR_LOCATION != null) {
            return GROOVY_CLASS_LOADER_FOR_LOCATION.parseClass(classText, location);
        } else {
            GroovyClassLoader classLoader = new GroovyClassLoader();
            Class<?> klass = classLoader.parseClass(classText, location);
            classLoader.close();
            return klass;
        }
    }

    /**
     * Parses a Groovy class from a text.
     * @param text as flexible string to parse
     * @return the corresponding class object
     * @throws IOException when parsing fails
     */
    public static Class<?> parseClass(String text) throws IOException {
        if (GROOVY_CLASS_LOADER != null) {
            return GROOVY_CLASS_LOADER.parseClass(text);
        } else {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class<?> classLoader = GROOVY_CLASS_LOADER.parseClass(text);
            groovyClassLoader.close();
            return classLoader;
        }
    }

    /**
     * Runs a Groovy script with a context argument.
     * <p>
     * A Groovy script can be either a stand-alone script or a method embedded in a script.
     * @param location  the location of the script file
     * @param methodName  the name of the method inside the script to be run,
     *                    if it is {@code null} consider the script as stand-alone
     * @param context  the context of execution which is in the case
     *                 of a method inside a script passed as an argument
     * @return the invocation result
     * @throws GeneralException when the script is not properly located
     */
    public static Object runScriptAtLocation(String location, String methodName, Map<String, Object> context)
            throws GeneralException {
        Script script = InvokerHelper.createScript(getScriptClassFromLocation(location), getBinding(context));
        return UtilValidate.isEmpty(methodName)
                ? script.run()
                : script.invokeMethod(methodName, new Object[] {context });
    }
}
