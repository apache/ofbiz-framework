/*
 * $Id: BSFEngine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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

import com.ibm.bsf.BSFException;
import com.ibm.bsf.BSFManager;

/**
 * BSF Service Engine
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @version    $Rev$
 * @since      2.1
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
        com.ibm.bsf.BSFEngine bsfEngine = null;        
        try {
            bsfEngine = mgr.loadScriptingEngine(modelService.engineName);
        } catch (BSFException e) {
            throw new GenericServiceException("Problems loading com.ibm.bsf.BSFEngine: " + modelService.engineName, e);
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
