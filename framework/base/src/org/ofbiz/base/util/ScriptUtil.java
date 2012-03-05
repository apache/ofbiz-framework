/*******************************************************************************
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
package org.ofbiz.base.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.cache.UtilCache;

/**
 * Scripting utility methods. This is a facade class that is used to connect OFBiz to JSR-223 scripting engines.
 *
 */
public final class ScriptUtil {

    public static final String module = ScriptUtil.class.getName();
    private static final UtilCache<String, CompiledScript> parsedScripts = UtilCache.createUtilCache("script.ParsedScripts", 0, 0, false);
    private static final Object[] EMPTY_ARGS = {};

    static {
        if (Debug.infoOn()) {
            ScriptEngineManager manager = new ScriptEngineManager();
            List<ScriptEngineFactory> engines = manager.getEngineFactories();
            if (engines.isEmpty()) {
                Debug.logInfo("No scripting engines were found.", module);
            } else {
                Debug.logInfo("The following " + engines.size() + " scripting engines were found:", module);
                for (ScriptEngineFactory engine : engines) {
                    Debug.logInfo("Engine name: " + engine.getEngineName(), module);
                    Debug.logInfo("  Version: " + engine.getEngineVersion(), module);
                    Debug.logInfo("  Language: " + engine.getLanguageName(), module);
                    List<String> extensions = engine.getExtensions();
                    if (extensions.size() > 0) {
                        Debug.logInfo("  Engine supports the following extensions:", module);
                        for (String e : extensions) {
                            Debug.logInfo("    " + e, module);
                        }
                    }
                    List<String> shortNames = engine.getNames();
                    if (shortNames.size() > 0) {
                        Debug.logInfo("  Engine has the following short names:", module);
                        for (String n : engine.getNames()) {
                            Debug.logInfo("    " + n, module);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a compiled script.
     * 
     * @param filePath Script path and file name.
     * @return The compiled script, or <code>null</code> if the script engine does not support compilation.
     * @throws FileNotFoundException
     * @throws IllegalArgumentException
     * @throws ScriptException
     * @throws MalformedURLException
     */
    public static CompiledScript compileScriptFile(String filePath) throws FileNotFoundException, ScriptException, MalformedURLException {
        Assert.notNull("filePath", filePath);
        CompiledScript script = parsedScripts.get(filePath);
        if (script == null) {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByExtension(getFileExtension(filePath));
            if (engine == null) {
                throw new IllegalArgumentException("The script type is not supported for location: " + filePath);
            }
            try {
                Compilable compilableEngine = (Compilable) engine;
                URL scriptUrl = FlexibleLocation.resolveLocation(filePath);
                FileReader reader = new FileReader(new File(scriptUrl.getFile()));
                script = compilableEngine.compile(reader);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Compiled script " + filePath + " using engine " + engine.getClass().getName(), module);
                }
            } catch (ClassCastException e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Script engine " + engine.getClass().getName() + " does not implement Compilable", module);
                }
            }
            if (script != null) {
                parsedScripts.putIfAbsent(filePath, script);
            }
        }
        return script;
    }

    /**
     * Returns a compiled script.
     * 
     * @param language
     * @param script
     * @return The compiled script, or <code>null</code> if the script engine does not support compilation.
     * @throws IllegalArgumentException
     * @throws ScriptException
     */
    public static CompiledScript compileScriptString(String language, String script) throws ScriptException {
        Assert.notNull("language", language, "script", script);
        String cacheKey = language.concat("://").concat(script);
        CompiledScript compiledScript = parsedScripts.get(cacheKey);
        if (compiledScript == null) {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName(language);
            if (engine == null) {
                throw new IllegalArgumentException("The script type is not supported for language: " + language);
            }
            try {
                Compilable compilableEngine = (Compilable) engine;
                compiledScript = compilableEngine.compile(script);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Compiled script [" + script + "] using engine " + engine.getClass().getName(), module);
                }
            } catch (ClassCastException e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Script engine " + engine.getClass().getName() + " does not implement Compilable", module);
                }
            }
            if (script != null) {
                parsedScripts.putIfAbsent(cacheKey, compiledScript);
            }
        }
        return compiledScript;
    }

    /**
     * Returns a <code>ScriptContext</code> that contains the members of <code>context</code>.
     * <p>If a <code>CompiledScript</code> instance is to be shared by multiple threads, then
     * each thread must create its own <code>ScriptContext</code> and pass it to the
     * <code>CompiledScript</code> eval method.</p>
     * 
     * @param context
     * @return
     */
    public static ScriptContext createScriptContext(Map<String, ? extends Object> context) {
        ScriptContext scriptContext = new SimpleScriptContext();
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.putAll(context);
        bindings.put("context", context);
        return scriptContext;
    }

    /**
     * Executes a script <code>String</code> and returns the result.
     * 
     * @param language
     * @param script
     * @param scriptClass
     * @param inputMap
     * @return The script result.
     * @throws Exception
     */
    public static Object evaluate(String language, String script, Class<?> scriptClass, Map<String, ? extends Object> inputMap) throws Exception {
        Assert.notNull("inputMap", inputMap);
        if (scriptClass != null) {
            return InvokerHelper.createScript(scriptClass, GroovyUtil.getBinding(inputMap)).run();
        }
        // TODO: Remove beanshell check when all beanshell code has been removed.
        if ("bsh".equals(language)) {
            return BshUtil.eval(script, UtilMisc.makeMapWritable(inputMap));
        }
        try {
            CompiledScript compiledScript = compileScriptString(language, script);
            if (compiledScript != null) {
                return executeScript(compiledScript, null, inputMap);
            }
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName(language);
            if (engine == null) {
                throw new IllegalArgumentException("The script type is not supported for language: " + language);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Begin processing script [" + script + "] using engine " + engine.getClass().getName(), module);
            }
            ScriptContext scriptContext = createScriptContext(inputMap);
            return engine.eval(script, scriptContext);
        } catch (Exception e) {
            String errMsg = "Error running " + language + " script [" + script + "]: " + e.toString();
            Debug.logWarning(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
    }

    /**
     * Executes a compiled script and returns the result.
     * 
     * @param script Compiled script.
     * @param functionName Optional function or method to invoke.
     * @param context Script execution context.
     * @return The script result.
     * @throws IllegalArgumentException
     */
    public static Object executeScript(CompiledScript script, String functionName, Map<String, ? extends Object> context) throws ScriptException, NoSuchMethodException {
        Assert.notNull("script", script, "context", context);
        ScriptContext scriptContext = createScriptContext(context);
        Object result = script.eval(scriptContext);
        if (UtilValidate.isNotEmpty(functionName)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Invoking function/method " + functionName, module);
            }
            ScriptEngine engine = script.getEngine();
            try {
                Invocable invocableEngine = (Invocable) engine;
                result = invocableEngine.invokeFunction(functionName, EMPTY_ARGS);
            } catch (ClassCastException e) {
                throw new ScriptException("Script engine " + engine.getClass().getName() + " does not support function/method invocations");
            }
        }
        return result;
    }

    /**
     * Executes the script at the specified location and returns the result.
     * 
     * @param filePath Script path and file name.
     * @param functionName Optional function or method to invoke.
     * @param context Script execution context.
     * @return The script result.
     * @throws IllegalArgumentException
     */
    public static Object executeScript(String filePath, String functionName, Map<String, ? extends Object> context) {
        Assert.notNull("filePath", filePath, "context", context);
        try {
            CompiledScript script = compileScriptFile(filePath);
            if (script != null) {
                return executeScript(script, functionName, context);
            }
            String fileExtension = getFileExtension(filePath);
            // TODO: Remove beanshell check when all beanshell code has been removed.
            if ("bsh".equals(fileExtension)) {
                return BshUtil.runBshAtLocation(filePath, context);
            } else {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByExtension(fileExtension);
                if (engine == null) {
                    throw new IllegalArgumentException("The script type is not supported for location: " + filePath);
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Begin processing script [" + script + "] using engine " + engine.getClass().getName(), module);
                }
                ScriptContext scriptContext = createScriptContext(context);
                URL scriptUrl = FlexibleLocation.resolveLocation(filePath);
                FileReader reader = new FileReader(new File(scriptUrl.getFile()));
                Object result = engine.eval(reader, scriptContext);
                if (UtilValidate.isNotEmpty(functionName)) {
                    try {
                        Invocable invocableEngine = (Invocable) engine;
                        result = invocableEngine.invokeFunction(functionName, EMPTY_ARGS);
                    } catch (ClassCastException e) {
                        throw new ScriptException("Script engine " + engine.getClass().getName() + " does not support function/method invocations");
                    }
                }
                return result;
            }
        } catch (Exception e) {
            String errMsg = "Error running script at location [" + filePath + "]: " + e.toString();
            Debug.logWarning(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
    }

    private static String getFileExtension(String filePath) {
        int pos = filePath.lastIndexOf(".");
        if (pos == -1) {
            throw new IllegalArgumentException("Extension missing in script file name: " + filePath);
        }
        return filePath.substring(pos + 1);
    }

    public static Class<?> parseScript(String language, String script) {
        Class<?> scriptClass = null;
        if ("groovy".equals(language)) {
            scriptClass = GroovyUtil.parseClass(script);
        }
        return scriptClass;
    }

    private ScriptUtil() {}
}
