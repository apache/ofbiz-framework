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
package org.ofbiz.service.engine;

import java.net.URL;
import java.util.Map;

import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * BeanShell Script Service Engine
 */
public final class BeanShellEngine extends GenericAsyncEngine {

    public static UtilCache scriptCache = new UtilCache("BeanShellScripts", 0, 0);

    public BeanShellEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }
    
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {
        Map result = runSync(localName, modelService, context);
    }
  
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {
        Object result = serviceInvoker(localName, modelService, context);

        if (result == null || !(result instanceof Map))
            throw new GenericServiceException("Service did not return expected result");
        return (Map) result;
    }

    // Invoke the BeanShell Script.
    private Object serviceInvoker(String localName, ModelService modelService, Map context) throws GenericServiceException {
        if (modelService.location == null || modelService.invoke == null)
            throw new GenericServiceException("Cannot locate service to invoke");

        // get the DispatchContext from the localName and ServiceDispatcher
        DispatchContext dctx = dispatcher.getLocalContext(localName);
        
        // get the classloader to use
        ClassLoader cl = null;

        if (dctx == null) {
            cl = this.getClass().getClassLoader();
        } else {
            cl = dctx.getClassLoader();
        }

        String location = this.getLocation(modelService);

        // source the script into a string
        String script = (String) scriptCache.get(localName + "_" + location);

        if (script == null) {
            synchronized (this) {
                script = (String) scriptCache.get(localName + "_" + location);
                if (script == null) {
                    URL scriptUrl = UtilURL.fromResource(location, cl);

                    if (scriptUrl != null) {
                        try {
                            HttpClient http = new HttpClient(scriptUrl);
                            script = http.get();
                        } catch (HttpClientException e) {
                            throw new GenericServiceException("Cannot read script from resource", e);
                        }
                    } else {
                        throw new GenericServiceException("Cannot read script, resource [" + location + "] not found");
                    }
                    if (script == null || script.length() < 2) {
                        throw new GenericServiceException("Null or empty script");
                    }
                    scriptCache.put(localName + "_" + location, script);
                }
            }
        }

        Interpreter bsh = new Interpreter();

        Map result = null;

        try {
            bsh.set("dctx", dctx); // set the dispatch context
            bsh.set("context", context); // set the parameter context used for both IN and OUT
            bsh.eval(script);
            Object bshResult = bsh.get("result");

            if ((bshResult != null) && (bshResult instanceof Map))
                context.putAll((Map) bshResult);
            result = modelService.makeValid(context, ModelService.OUT_PARAM);
        } catch (EvalError e) {
            throw new GenericServiceException("BeanShell script threw an exception", e);
        }
        return result;
    }

}

