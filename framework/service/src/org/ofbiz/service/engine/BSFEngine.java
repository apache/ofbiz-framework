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
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

/**
 * BSF Service Engine
 */
public class BSFEngine extends GenericAsyncEngine {
    
    public static final String module = BSFEngine.class.getName();
    public static UtilCache scriptCache = new UtilCache("BSFScripts", 0, 0);
            
    public BSFEngine(ServiceDispatcher dispatcher) {
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
    
    // Invoke the BSF Script.
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

        // create the manager object and set the classloader
        BSFManager mgr = new BSFManager();
        mgr.setClassLoader(cl);

        mgr.registerBean("dctx", dctx);
        mgr.registerBean("context", context);
        
        // pre-load the engine to make sure we were called right
        org.apache.bsf.BSFEngine bsfEngine = null;        
        try {
            bsfEngine = mgr.loadScriptingEngine(modelService.engineName);
        } catch (BSFException e) {
            throw new GenericServiceException("Problems loading org.apache.bsf.BSFEngine: " + modelService.engineName, e);
        }
        
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
        
        // now invoke the script
        try {
            bsfEngine.exec(location, 0, 0, script);
        } catch (BSFException e) {
            throw new GenericServiceException("Script invocation error", e);
        }
        
        return mgr.lookupBean("response");                                            
    }
}
