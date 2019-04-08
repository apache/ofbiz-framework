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
public class GroovyUtil {

    public static final String module = GroovyUtil.class.getName();

    private static final UtilCache<String, Class<?>> parsedScripts = UtilCache.createUtilCache("script.GroovyLocationParsedCache", 0, 0, false);

    private static final GroovyClassLoader groovyScriptClassLoader;
    static {
        GroovyClassLoader groovyClassLoader = null;
        String scriptBaseClass = UtilProperties.getPropertyValue("groovy", "scriptBaseClass");
        if (!scriptBaseClass.isEmpty()) {
            CompilerConfiguration conf = new CompilerConfiguration();
            conf.setScriptBaseClass(scriptBaseClass);
            groovyClassLoader = new GroovyClassLoader(GroovyUtil.class.getClassLoader(), conf);
        }
        groovyScriptClassLoader = groovyClassLoader;
        /*
         *  With the implementation of @BaseScript annotations (introduced with Groovy 2.3.0) something was broken
         *  in the CompilerConfiguration.setScriptBaseClass method and an error is thrown when our scripts are executed;
         *  the workaround is to execute at startup a script containing the @BaseScript annotation.
         */
        try {
            GroovyUtil.runScriptAtLocation("component://base/config/GroovyInit.groovy", null, null);
        } catch(Exception e) {
            Debug.logWarning("The following error occurred during the initialization of Groovy: " + e.getMessage(), module);
        }
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
        if (expression == null || expression.equals("")) {
            Debug.logError("Groovy Evaluation error. Empty expression", module);
            return null;
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Evaluating -- " + expression, module);
            Debug.logVerbose("Using Context -- " + context, module);
        }
        try {
            GroovyShell shell = new GroovyShell(getBinding(context, expression));
            o = shell.evaluate(StringUtil.convertOperatorSubstitutions(expression));
            if (Debug.verboseOn()) {
                Debug.logVerbose("Evaluated to -- " + o, module);
            }
            // read back the context info
            Binding binding = shell.getContext();
            context.putAll(binding.getVariables());
        } catch (CompilationFailedException e) {
            Debug.logError(e, "Groovy Evaluation error.", module);
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
     * 
     * @param context A <code>Map</code> containing initial variables
     * @return A <code>Binding</code> instance
     */
    public static Binding getBinding(Map<String, Object> context, String expression) {
        Map<String, Object> vars = new HashMap<String, Object>();
        if (context != null) {
            vars.putAll(context);
            if (UtilValidate.isNotEmpty(expression)) {
                //analyse expression to find variables by split non alpha, ignoring "_" to allow my_variable usage
                String [] variables = expression.split("[\\P{Alpha}&&[^_]]+");
                for (String variable: variables)
                    if(!vars.containsKey(variable)) vars.put(variable, null);
            }
            vars.put("context", context);
            if (vars.get(ScriptUtil.SCRIPT_HELPER_KEY) == null) {
                ScriptContext scriptContext = ScriptUtil.createScriptContext(context);
                ScriptHelper scriptHelper = (ScriptHelper)scriptContext.getAttribute(ScriptUtil.SCRIPT_HELPER_KEY);
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
            Class<?> scriptClass = parsedScripts.get(location);
            if (scriptClass == null) {
                URL scriptUrl = FlexibleLocation.resolveLocation(location);
                if (scriptUrl == null) {
                    throw new GeneralException("Script not found at location [" + location + "]");
                }
                if (groovyScriptClassLoader != null) {
                    scriptClass = parseClass(scriptUrl.openStream(), location, groovyScriptClassLoader);
                } else {
                    scriptClass = parseClass(scriptUrl.openStream(), location);
                }
                Class<?> scriptClassCached = parsedScripts.putIfAbsent(location, scriptClass);
                if (scriptClassCached == null) { // putIfAbsent returns null if the class is added to the cache
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Cached Groovy script at: " + location, module);
                    }
                } else {
                    // the newly parsed script is discarded and the one found in the cache (that has been created by a concurrent thread in the meantime) is used
                    scriptClass = scriptClassCached;
                }
            }
            return scriptClass;
        } catch (Exception e) {
            throw new GeneralException("Error loading Groovy script at [" + location + "]: ", e);
        }
    }

    public static Class<?> loadClass(String path) throws ClassNotFoundException, IOException {
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
        Class<?> classLoader = groovyClassLoader.loadClass(path);
        groovyClassLoader.close();
        return classLoader;
    }

    public static Class<?> parseClass(InputStream in, String location) throws IOException {
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
        Class<?> classLoader = groovyClassLoader.parseClass(UtilIO.readString(in), location);
        groovyClassLoader.close();
        return classLoader;
    }
    public static Class<?> parseClass(InputStream in, String location, GroovyClassLoader groovyClassLoader) throws IOException {
        return groovyClassLoader.parseClass(UtilIO.readString(in), location);
    }

    public static Class<?> parseClass(String text) throws IOException {
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
        Class<?> classLoader = groovyClassLoader.parseClass(text);
        groovyClassLoader.close();
        return classLoader;
    }

    public static Object runScriptAtLocation(String location, String methodName, Map<String, Object> context) throws GeneralException {
        Script script = InvokerHelper.createScript(getScriptClassFromLocation(location), getBinding(context));
        Object result = null;
        if (UtilValidate.isEmpty(methodName)) {
            result = script.run();
        } else {
            result = script.invokeMethod(methodName, new Object[] { context });
        }
        return result;
    }

    private GroovyUtil() {}
}
