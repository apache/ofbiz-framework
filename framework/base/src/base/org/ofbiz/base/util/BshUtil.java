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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    protected static Map masterClassManagers = new HashMap();
    public static UtilCache parsedScripts = new UtilCache("script.BshLocationParsedCache", 0, 0, false);
    
    /**
     * Evaluate a BSH condition or expression
     * @param expression The expression to evaluate
     * @param context The context to use in evaluation (re-written)
     * @return Object The result of the evaluation 
     * @throws EvalError
     */
    public static final Object eval(String expression, Map context) throws EvalError {
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
            for (int x = 0; x < varNames.length; x++) {
                context.put(varNames[x], bsh.get(varNames[x]));
            }
        } catch (EvalError e) {
            Debug.logError(e, "BSH Evaluation error.", module);
            throw e;
        }
        return o;
    }
    
    public static Interpreter makeInterpreter(Map context) throws EvalError {
        Interpreter bsh = getMasterInterpreter(null);
        // Set the context for the condition
        if (context != null) {
            Set keySet = context.keySet();
            Iterator i = keySet.iterator();
            while (i.hasNext()) {
                Object key = i.next();
                Object value = context.get(key);
                bsh.set((String) key, value);
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
        BshClassManager master = (BshClassManager) BshUtil.masterClassManagers.get(classLoader);
        if (master == null) {
            synchronized (OfbizBshBsfEngine.class) {
                master = (BshClassManager) BshUtil.masterClassManagers.get(classLoader);
                if (master == null) {
                    master = BshClassManager.createClassManager();
                    master.setClassLoader(classLoader);
                    BshUtil.masterClassManagers.put(classLoader, master);
                }
            }
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
    
    public static Object runBshAtLocation(String location, Map context) throws GeneralException {
        try {
            Interpreter interpreter = makeInterpreter(context);
            
            Interpreter.ParsedScript script = null;
            script = (Interpreter.ParsedScript) parsedScripts.get(location);
            if (script == null) {
                synchronized (OfbizBshBsfEngine.class) {
                    script = (Interpreter.ParsedScript) parsedScripts.get(location);
                    if (script == null) {
                        URL scriptUrl = FlexibleLocation.resolveLocation(location);
                        if (scriptUrl == null) {
                            throw new GeneralException("Could not find bsh script at [" + location + "]");
                        }
                        Reader scriptReader = new InputStreamReader(scriptUrl.openStream());
                        script = interpreter.parseScript(location, scriptReader);
                        if (Debug.verboseOn()) Debug.logVerbose("Caching BSH script at: " + location, module);
                        parsedScripts.put(location, script);
                    }
                }
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
                Debug.logWarning(ee, "WARNING: no cause (from getCause) found for BSH EvalError: " + ee.toString(), module);
                t = ee;
            } else {
                Debug.logError(t, "ERROR: Got cause (from getCause) for BSH EvalError: " + ee.toString(), module);
            }
            
            String errMsg = "Error running BSH script at [" + location + "], line [" + ee.getErrorLineNumber() + "]: " + t.toString();
            // don't log the full exception, just the main message; more detail logged later
            throw new GeneralException(errMsg, t);
        }
    }
}
