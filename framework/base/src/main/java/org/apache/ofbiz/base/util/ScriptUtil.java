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
package org.apache.ofbiz.base.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.base.util.ScriptHelper;
import org.apache.ofbiz.common.scripting.ScriptHelperImpl;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Scripting utility methods. This is a facade class that is used to connect OFBiz to JSR-223 scripting engines.
 * <p><b>Important:</b> To avoid a lot of <code>Map</code> copying, all methods that accept a context
 * <code>Map</code> argument will pass that <code>Map</code> directly to the scripting engine. Any variables that
 * are declared or modified in the script will affect the original <code>Map</code>. Client code that wishes to preserve
 * the state of the <code>Map</code> argument should pass a copy of the <code>Map</code>.</p>
 *
 */
public final class ScriptUtil {

    public static final String module = ScriptUtil.class.getName();
    /** The screen widget context map bindings key. */
    public static final String WIDGET_CONTEXT_KEY = "widget";
    /** The service/servlet/request parameters map bindings key. */
    public static final String PARAMETERS_KEY = "parameters";
    /** The result map bindings key. */
    public static final String RESULT_KEY = "result";
    /** The <code>ScriptHelper</code> key. */
    public static final String SCRIPT_HELPER_KEY = "ofbiz";
    private static final UtilCache<String, CompiledScript> parsedScripts = UtilCache.createUtilCache("script.ParsedScripts", 0, 0, false);
    private static final Object[] EMPTY_ARGS = {};
    /** A set of script names - derived from the JSR-223 scripting engines. */
    public static final Set<String> SCRIPT_NAMES;

    static {
        Set<String> writableScriptNames = new HashSet<>();
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
                    for (String name : engine.getNames()) {
                        writableScriptNames.add(name);
                        Debug.logInfo("    " + name, module);
                    }
                }
            }
        }
        SCRIPT_NAMES = Collections.unmodifiableSet(writableScriptNames);
    }

    /**
     * Returns a compiled script.
     *
     * @param filePath Script path and file name.
     * @return The compiled script, or <code>null</code> if the script engine does not support compilation.
     * @throws IllegalArgumentException
     * @throws ScriptException
     * @throws IOException
     */
    public static CompiledScript compileScriptFile(String filePath) throws ScriptException, IOException {
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(scriptUrl.openStream(), UtilIO
                        .getUtf8()));
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
            if (compiledScript != null) {
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
    public static ScriptContext createScriptContext(Map<String, Object> context) {
        Assert.notNull("context", context);
        Map<String, Object> localContext = new HashMap<>(context);
        localContext.put(WIDGET_CONTEXT_KEY, context);
        localContext.put("context", context);
        ScriptContext scriptContext = new SimpleScriptContext();
        ScriptHelper helper = new ScriptHelperImpl(scriptContext);
        localContext.put(SCRIPT_HELPER_KEY, helper);
        Bindings bindings = new SimpleBindings(localContext);
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return scriptContext;
    }

    /**
     * Returns a <code>ScriptContext</code> that contains the members of <code>context</code>.
     * <p>If a <code>CompiledScript</code> instance is to be shared by multiple threads, then
     * each thread must create its own <code>ScriptContext</code> and pass it to the
     * <code>CompiledScript</code> eval method.</p>
     *
     * @param context
     * @param protectedKeys
     * @return
     */
    public static ScriptContext createScriptContext(Map<String, Object> context, Set<String> protectedKeys) {
        Assert.notNull("context", context, "protectedKeys", protectedKeys);
        Map<String, Object> localContext = new HashMap<>(context);
        localContext.put(WIDGET_CONTEXT_KEY, context);
        localContext.put("context", context);
        ScriptContext scriptContext = new SimpleScriptContext();
        Bindings bindings = new ProtectedBindings(localContext, Collections.unmodifiableSet(protectedKeys));
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        localContext.put(SCRIPT_HELPER_KEY, new ScriptHelperImpl(scriptContext));
        return scriptContext;
    }

    /**
     * Executes a script <code>String</code> and returns the result.
     *
     * @param language
     * @param script
     * @param scriptClass
     * @param context
     * @return The script result.
     * @throws Exception
     */
    public static Object evaluate(String language, String script, Class<?> scriptClass, Map<String, Object> context) throws Exception {
        Assert.notNull("context", context);
        if (scriptClass != null) {
            return InvokerHelper.createScript(scriptClass, GroovyUtil.getBinding(context)).run();
        }
        try {
            CompiledScript compiledScript = compileScriptString(language, script);
            if (compiledScript != null) {
                return executeScript(compiledScript, null, createScriptContext(context), null);
            }
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName(language);
            if (engine == null) {
                throw new IllegalArgumentException("The script type is not supported for language: " + language);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Begin processing script [" + script + "] using engine " + engine.getClass().getName(), module);
            }
            ScriptContext scriptContext = createScriptContext(context);
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
     * @param scriptContext Script execution context.
     * @return The script result.
     * @throws IllegalArgumentException
     */
    public static Object executeScript(CompiledScript script, String functionName, ScriptContext scriptContext, Object[] args) throws ScriptException, NoSuchMethodException {
        Assert.notNull("script", script, "scriptContext", scriptContext);
        Object result = script.eval(scriptContext);
        if (UtilValidate.isNotEmpty(functionName)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Invoking function/method " + functionName, module);
            }
            ScriptEngine engine = script.getEngine();
            try {
                Invocable invocableEngine = (Invocable) engine;
                result = invocableEngine.invokeFunction(functionName, args == null ? EMPTY_ARGS : args);
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
    public static Object executeScript(String filePath, String functionName, Map<String, Object> context) {
        return executeScript(filePath, functionName, context, new Object[] { context });
    }

    /**
     * Executes the script at the specified location and returns the result.
     *
     * @param filePath Script path and file name.
     * @param functionName Optional function or method to invoke.
     * @param context Script execution context.
     * @param args Function/method arguments.
     * @return The script result.
     * @throws IllegalArgumentException
     */
    public static Object executeScript(String filePath, String functionName, Map<String, Object> context, Object[] args) {
        try {
            // Enabled to run Groovy data preparation scripts using GroovyUtil rather than the generic JSR223 that doesn't support debug mode
            // and does not return a stack trace with line number on error
            if (filePath.endsWith(".groovy")) {
                return GroovyUtil.runScriptAtLocation(filePath, functionName, context);
            }
            return executeScript(filePath, functionName, createScriptContext(context), args);
        } catch (Exception e) {
            String errMsg = "Error running script at location [" + filePath + "]: " + e.toString();
            Debug.logWarning(e, errMsg, module);
            throw new IllegalArgumentException(errMsg, e);
        }
    }

    /**
     * Executes the script at the specified location and returns the result.
     *
     * @param filePath Script path and file name.
     * @param functionName Optional function or method to invoke.
     * @param scriptContext Script execution context.
     * @param args Function/method arguments.
     * @return The script result.
     * @throws ScriptException
     * @throws NoSuchMethodException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static Object executeScript(String filePath, String functionName, ScriptContext scriptContext, Object[] args) throws ScriptException, NoSuchMethodException, IOException {
        Assert.notNull("filePath", filePath, "scriptContext", scriptContext);
        scriptContext.setAttribute(ScriptEngine.FILENAME, filePath, ScriptContext.ENGINE_SCOPE);
        if (functionName == null) {
            // The Rhino script engine will not work when invoking a function on a compiled script.
            // The test for null can be removed when the engine is fixed.
            CompiledScript script = compileScriptFile(filePath);
            if (script != null) {
                return executeScript(script, null, scriptContext, args);
            }
        }
        String fileExtension = getFileExtension(filePath);
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByExtension(fileExtension);
        if (engine == null) {
            throw new IllegalArgumentException("The script type is not supported for location: " + filePath);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Begin processing script [" + filePath + "] using engine " + engine.getClass().getName(), module);
        }
        engine.setContext(scriptContext);
        URL scriptUrl = FlexibleLocation.resolveLocation(filePath);
        try (
                InputStreamReader reader = new InputStreamReader(new FileInputStream(scriptUrl.getFile()), UtilIO
                        .getUtf8());) {
            Object result = engine.eval(reader);
            if (UtilValidate.isNotEmpty(functionName)) {
                try {
                    Invocable invocableEngine = (Invocable) engine;
                    result = invocableEngine.invokeFunction(functionName, args == null ? EMPTY_ARGS : args);
                } catch (ClassCastException e) {
                    throw new ScriptException("Script engine " + engine.getClass().getName()
                            + " does not support function/method invocations");
                }
            }
            return result;
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
            try {
                scriptClass = GroovyUtil.parseClass(script);
            } catch (IOException e) {
                Debug.logError(e, module);
                return null;
            }
        }
        return scriptClass;
    }

    private ScriptUtil() {}

    private static final class ProtectedBindings implements Bindings {
        private final Map<String, Object> bindings;
        private final Set<String> protectedKeys;
        private ProtectedBindings(Map<String, Object> bindings, Set<String> protectedKeys) {
            this.bindings = bindings;
            this.protectedKeys = protectedKeys;
        }
        @Override
        public void clear() {
            for (String key : bindings.keySet()) {
                if (!protectedKeys.contains(key)) {
                    bindings.remove(key);
                }
            }
        }
        @Override
        public boolean containsKey(Object key) {
            return bindings.containsKey(key);
        }
        @Override
        public boolean containsValue(Object value) {
            return bindings.containsValue(value);
        }
        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            return bindings.entrySet();
        }
        @Override
        public boolean equals(Object o) {
            return bindings.equals(o);
        }
        @Override
        public Object get(Object key) {
            return bindings.get(key);
        }
        @Override
        public int hashCode() {
            return bindings.hashCode();
        }
        @Override
        public boolean isEmpty() {
            return bindings.isEmpty();
        }
        @Override
        public Set<String> keySet() {
            return bindings.keySet();
        }
        @Override
        public Object put(String key, Object value) {
            Assert.notNull("key", key);
            if (protectedKeys.contains(key)) {
                UnsupportedOperationException e = new UnsupportedOperationException("Variable " + key + " is read-only");
                Debug.logWarning(e, module);
                throw e;
            }
            return bindings.put(key, value);
        }
        @Override
        public void putAll(Map<? extends String, ? extends Object> map) {
            for (Map.Entry<? extends String, ? extends Object> entry : map.entrySet()) {
                Assert.notNull("key", entry.getKey());
                if (!protectedKeys.contains(entry.getKey())) {
                    bindings.put(entry.getKey(), entry.getValue());
                }
            }
        }
        @Override
        public Object remove(Object key) {
            if (protectedKeys.contains(key)) {
                UnsupportedOperationException e = new UnsupportedOperationException("Variable " + key + " is read-only");
                Debug.logWarning(e, module);
                throw e;
            }
            return bindings.remove(key);
        }
        @Override
        public int size() {
            return bindings.size();
        }
        @Override
        public Collection<Object> values() {
            return bindings.values();
        }
    }
}
