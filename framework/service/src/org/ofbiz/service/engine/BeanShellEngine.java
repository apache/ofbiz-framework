/*
 * $Id: BeanShellEngine.java 5462 2005-08-05 18:35:48Z jonesde $
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @version    $Rev$
 * @since      2.0
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

