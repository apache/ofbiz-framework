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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.cache.UtilCache;

import bsh.BshClassManager;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.ParseException;

/**
 * BshUtil - BeanShell Utilities
 *
 */
public final class BshUtil {

    public static final String module = BshUtil.class.getName();

    private static ConcurrentHashMap<ClassLoader, BshClassManager> masterClassManagers = new ConcurrentHashMap<ClassLoader, BshClassManager>();
    private static final UtilCache<String, Interpreter.ParsedScript> parsedScripts = UtilCache.createUtilCache("script.BshLocationParsedCache", 0, 0, false);

    private BshUtil() {}

    /**
     * Evaluate a BSH condition or expression
     * @param expression The expression to evaluate
     * @param context The context to use in evaluation (re-written)
     * @return Object The result of the evaluation
     * @throws EvalError
     */
    public static final Object eval(String expression, Map<String, Object> context) throws EvalError {
        Object o = null;
        if (expression == null || expression.equals("")) {
            Debug.logError("BSH Evaluation error. Empty expression", module);
            return null;
        }

        if (Debug.verboseOn())
            Debug.logVerbose("Evaluating -- " + expression, module);
        if (Debug.verboseOn())
            Debug.logVerbose("Using Context -- " + context, module);

        try {
            Interpreter bsh = makeInterpreter(context);
            // evaluate the expression
            o = bsh.eval(expression);
            if (Debug.verboseOn())
                Debug.logVerbose("Evaluated to -- " + o, module);

            // read back the context info
            NameSpace ns = bsh.getNameSpace();
            String[] varNames = ns.getVariableNames();
            for (String varName: varNames) {
                context.put(varName, bsh.get(varName));
            }
        } catch (EvalError e) {
            Debug.logError(e, "BSH Evaluation error.", module);
            throw e;
        }
        return o;
    }

    public static Interpreter makeInterpreter(Map<String, ? extends Object> context) throws EvalError {
        Interpreter bsh = getMasterInterpreter(null);
        // Set the context for the condition
        if (context != null) {
            for (Map.Entry<String, ? extends Object> entry: context.entrySet()) {
                bsh.set(entry.getKey(), entry.getValue());
            }

            // include the context itself in for easier access in the scripts
            bsh.set("context", context);
        }

        return bsh;
    }

    public static Interpreter getMasterInterpreter(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        //find the "master" BshClassManager for this classpath
        BshClassManager master = BshUtil.masterClassManagers.get(classLoader);
        if (master == null) {
            master = BshClassManager.createClassManager();
            master.setClassLoader(classLoader);
            BshUtil.masterClassManagers.putIfAbsent(classLoader, master);
            master = BshUtil.masterClassManagers.get(classLoader);
        }

        if (master != null) {
            Interpreter interpreter = new Interpreter(new StringReader(""), System.out, System.err,
                    false, new NameSpace(master, "global"), null, null);
            return interpreter;
        } else {
            Interpreter interpreter = new Interpreter();
            interpreter.setClassLoader(classLoader);
            return interpreter;
        }
    }

    public static Object runBshAtLocation(String location, Map<String, ? extends Object> context) throws GeneralException {
        try {
            Interpreter interpreter = makeInterpreter(context);

            Interpreter.ParsedScript script = null;
            script = parsedScripts.get(location);
            if (script == null) {
                URL scriptUrl = FlexibleLocation.resolveLocation(location);
                if (scriptUrl == null) {
                    throw new GeneralException("Could not find bsh script at [" + location + "]");
                }
                Reader scriptReader = new InputStreamReader(scriptUrl.openStream());
                script = interpreter.parseScript(location, scriptReader);
                if (Debug.verboseOn()) Debug.logVerbose("Caching BSH script at: " + location, module);
                script = parsedScripts.putIfAbsentAndGet(location, script);
            }

            return interpreter.evalParsedScript(script);
        } catch (MalformedURLException e) {
            String errMsg = "Error loading BSH script at [" + location + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new GeneralException(errMsg, e);
        } catch (ParseException e) {
            String errMsg = "Error parsing BSH script at [" + location + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new GeneralException(errMsg, e);
        } catch (IOException e) {
            String errMsg = "Error loading BSH script at [" + location + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new GeneralException(errMsg, e);
        } catch (EvalError ee) {
            Throwable t = ee.getCause();
            if (t == null) {
                Debug.logWarning(ee, "No cause (from getCause) found for BSH EvalError: " + ee.toString(), module);
                t = ee;
            } else {
                Debug.logError(t, "Got cause (from getCause) for BSH EvalError: " + ee.toString(), module);
            }

            String errMsg = "Error running BSH script at [" + location + "], line [" + ee.getErrorLineNumber() + "]: " + t.toString();
            // don't log the full exception, just the main message; more detail logged later
            throw new GeneralException(errMsg, t);
        }
    }
}
