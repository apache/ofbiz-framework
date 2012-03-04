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

import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Map;

public final class ScriptUtil {

    public static final String module = ScriptUtil.class.getName();
    private static final Object[] EMPTY_ARGS = {};

    /* TODO: the "evaluate" and "executeScript" method method could be enhanced to implement JSR-223 using code like:
              import javax.script.ScriptEngineManager;
              import javax.script.ScriptEngine;
              ...
              ScriptEngineManager manager = new ScriptEngineManager();
              ScriptEngine scriptEngine = manager.getEngineByExtension(location.substring(location.indexOf(".") + 1));
              ...
              Object result = scriptEngine.eval(scriptFileReader, scriptContext);

           However it may make more sense to keep a custom way to load and execute Groovy scripts and implement JSR-223
           for the other scripting languages: in this way the OFBiz framework will support any script language with JSR-223
           but will still have specialized support for Groovy (where we could/should inject OFBiz specific utility methods
           and create a security sandbox for Groovy dynamic code).
    */
    public static Object evaluate(String language, String script, Class<?> scriptClass, Map inputMap) throws Exception {
        /*
            TODO: for JSR-223 we could use:
              ScriptEngine scriptEngine = manager.getEngineByName(location);
        */
        Object result = null;
        if ("groovy".equals(language)) {
            if (scriptClass == null) {
                scriptClass = ScriptUtil.parseScript(language, script);
            }
            if (scriptClass != null) {
                result = InvokerHelper.createScript(scriptClass, GroovyUtil.getBinding(inputMap)).run();
            }
        } else if ("bsh".equals(language)) {
            result = BshUtil.eval(script, UtilMisc.makeMapWritable(inputMap));
        }
        return result;
    }

    public static void executeScript(String location, String method, Map<String, Object> context) {
        /*
            TODO: for JSR-223 we could use:
              ScriptEngine scriptEngine = manager.getEngineByExtension(location.substring(location.indexOf(".") + 1));
        */
        if (location.endsWith(".bsh")) {
            try {
                BshUtil.runBshAtLocation(location, context);
            } catch (GeneralException e) {
                String errMsg = "Error running BSH script at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        } else if (location.endsWith(".groovy")) {
            try {
                groovy.lang.Script script = InvokerHelper.createScript(GroovyUtil.getScriptClassFromLocation(location), GroovyUtil.getBinding(context));
                if (UtilValidate.isEmpty(method)) {
                    script.run();
                } else {
                    script.invokeMethod(method, EMPTY_ARGS);
                }
            } catch (GeneralException e) {
                String errMsg = "Error running Groovy script at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        } else {
            throw new IllegalArgumentException("The script type is not yet support for location:" + location);
        }
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
