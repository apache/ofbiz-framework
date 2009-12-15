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
package org.ofbiz.base.util;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.cache.UtilCache;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * GroovyUtil - Groovy Utilities
 *
 */
public class GroovyUtil {

    public static final String module = GroovyUtil.class.getName();

    @SuppressWarnings("unchecked")
    public static UtilCache<String, Class> parsedScripts = UtilCache.createUtilCache("script.GroovyLocationParsedCache", 0, 0, false);

    public static Class loadClass(String path) throws ClassNotFoundException {
        return new GroovyClassLoader().loadClass(path);
    }

    public static Class parseClass(String text) {
        return new GroovyClassLoader().parseClass(text);
    }

    public static Class parseClass(String text, String location) {
        return new GroovyClassLoader().parseClass(text, location);
    }

    public static Class parseClass(InputStream in, String location) throws IOException {
        return new GroovyClassLoader().parseClass(UtilIO.readString(in), location);
    }

    public static Binding getBinding(Map<String, ? extends Object> context) {
        Binding binding = new Binding();
        if (context != null) {
            Set<String> keySet = context.keySet();
            for (Object key : keySet) {
                binding.setVariable((String) key, context.get(key));
            }

            // include the context itself in for easier access in the scripts
            binding.setVariable("context", context);
        }

        return binding;
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
            GroovyShell shell = new GroovyShell(getBinding(context));
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

    @SuppressWarnings("unchecked")
    public static Object runScriptFromClasspath(String script, Map<String,Object> context) throws GeneralException {
        try {
            Class scriptClass = parsedScripts.get(script);
            if (scriptClass == null) {
                scriptClass = loadClass(script);
                if (Debug.verboseOn()) Debug.logVerbose("Caching Groovy script: " + script, module);
                parsedScripts.put(script, scriptClass);
            }

            return InvokerHelper.createScript(scriptClass, getBinding(context)).run();
        } catch (CompilationFailedException e) {
            String errMsg = "Error loading Groovy script [" + script + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        } catch (ClassNotFoundException e) {
            String errMsg = "Error loading Groovy script [" + script + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object runScriptAtLocation(String location, Map<String, Object> context) throws GeneralException {
        try {
            Class scriptClass = parsedScripts.get(location);
            if (scriptClass == null) {
                URL scriptUrl = FlexibleLocation.resolveLocation(location);
                if (scriptUrl == null) {
                    throw new GeneralException("Script not found at location [" + location + "]");
                }

                scriptClass = parseClass(scriptUrl.openStream(), location);
                if (Debug.verboseOn()) Debug.logVerbose("Caching Groovy script at: " + location, module);
                parsedScripts.put(location, scriptClass);
            }

            return InvokerHelper.createScript(scriptClass, getBinding(context)).run();

            /* This code is just to test performance of the parsed versus unparsed approaches
            long startTimeParsed = System.currentTimeMillis();
            InvokerHelper.createScript(scriptClass, getBinding(context)).run();
            if (Debug.timingOn()) Debug.logTiming("Ran parsed groovy script in [" + (System.currentTimeMillis() - startTimeParsed) + "]ms at: " + location, module);
            */

            /* NOTE DEJ20080526: this approach uses a pre-parsed script but it is not thread safe
             *
             * the groovy Script object contains both the parsed script AND the context, which is weird when trying to run a cached Script
             * there is no available clone() method on the Script object, so we can't clone and set the context/binding to get around thread-safe issues
            Script script = parsedScripts.get(location);
            if (script == null) {
                URL scriptUrl = FlexibleLocation.resolveLocation(location);
                if (scriptUrl == null) {
                    throw new GeneralException("Script not found at location [" + location + "]");
                }

                script = emptyGroovyShell.parse(scriptUrl.openStream(), scriptUrl.getFile());
                if (Debug.verboseOn()) Debug.logVerbose("Caching Groovy script at: " + location, module);
                parsedScripts.put(location, script);
            }

            script.setBinding(getBinding(context));
            return script.run();
             */

            /* NOTE DEJ20080527: this approach works but only caches script text, not the parsed script
            public static UtilCache<String, String> sourceScripts = UtilCache.createUtilCache("script.GroovyLocationSourceCache", 0, 0, false);

            public static GroovyShell emptyGroovyShell = new GroovyShell();
            String scriptString = sourceScripts.get(location);
            if (scriptString == null) {
                URL scriptUrl = FlexibleLocation.resolveLocation(location);
                if (scriptUrl == null) {
                    throw new GeneralException("Script not found at location [" + location + "]");
                }

                scriptString = UtilURL.readUrlText(scriptUrl);

                if (Debug.verboseOn()) Debug.logVerbose("Caching Groovy script source at: " + location, module);
                sourceScripts.put(location, scriptString);
            }

            long startTime = System.currentTimeMillis();
            Script script = emptyGroovyShell.parse(scriptString, location);
            script.setBinding(getBinding(context));
            Object scriptResult = script.run();
            if (Debug.timingOn()) Debug.logTiming("Parsed and ran groovy script in [" + (System.currentTimeMillis() - startTime) + "]ms at: " + location, module);

            return scriptResult;
             */
        } catch (MalformedURLException e) {
            String errMsg = "Error loading Groovy script at [" + location + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        } catch (IOException e) {
            String errMsg = "Error loading Groovy script at [" + location + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        } catch (CompilationFailedException e) {
            String errMsg = "Error loading Groovy script at [" + location + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        }
    }
}
