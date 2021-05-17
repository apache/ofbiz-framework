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
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
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
    private static final UtilCache<String, Class<?>> PARSED_SCRIPTS = UtilCache.createUtilCache("script.GroovyLocationParsedCache", 0, 0, false);
    private static final GroovyClassLoader GROOVY_CLASS_LOADER;

    private GroovyUtil() { }

    static {
        GroovyClassLoader groovyClassLoader = null;
        String scriptBaseClass = UtilProperties.getPropertyValue("groovy", "scriptBaseClass");
        if (!scriptBaseClass.isEmpty()) {
            CompilerConfiguration conf = new CompilerConfiguration();
            conf.setScriptBaseClass(scriptBaseClass);
            groovyClassLoader = new GroovyClassLoader(GroovyUtil.class.getClassLoader(), conf);
        }
        GROOVY_CLASS_LOADER = groovyClassLoader;
    }

    /**
     * Evaluate a Groovy condition or expression
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
            GroovyShell shell = new GroovyShell(getBinding(context, expression));
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
    public static Binding getBinding(Map<String, Object> context, String expression) {
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
                if (scriptUrl == null) {
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
        if (GROOVY_CLASS_LOADER != null) {
            return GROOVY_CLASS_LOADER.parseClass(classText, location);
        } else {
            GroovyClassLoader classLoader = new GroovyClassLoader();
            Class<?> klass = classLoader.parseClass(classText, location);
            classLoader.close();
            return klass;
        }
    }

    public static Class<?> parseClass(String text) throws IOException {
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
        Class<?> classLoader = groovyClassLoader.parseClass(text);
        groovyClassLoader.close();
        return classLoader;
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
