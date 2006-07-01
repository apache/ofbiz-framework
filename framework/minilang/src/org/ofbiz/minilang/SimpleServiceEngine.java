/*
 * $Id: SimpleServiceEngine.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang;

import java.util.Map;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.engine.GenericAsyncEngine;

/**
 * Standard Java Static Method Service Engine
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public final class SimpleServiceEngine extends GenericAsyncEngine {

    /** Creates new Engine */
    public SimpleServiceEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /** Run the service synchronously and IGNORE the result
     * @param context Map of name, value pairs composing the context
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {        
        Map result = runSync(localName, modelService, context);
    }

    /** Run the service synchronously and return the result
     * @param context Map of name, value pairs composing the context
     * @return Map of name, value pairs composing the result
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {        
        Object result = serviceInvoker(localName, modelService, context);
        if (result == null || !(result instanceof Map))
            throw new GenericServiceException("Service did not return expected result");
        return (Map) result;
    }

    // Invoke the simple method from a service context
    private Object serviceInvoker(String localName, ModelService modelService, Map context) throws GenericServiceException {        
        // static java service methods should be: public Map methodName(DispatchContext dctx, Map context)
        DispatchContext dctx = dispatcher.getLocalContext(localName);

        // check the package and method names
        if (modelService.location == null || modelService.invoke == null)
            throw new GenericServiceException("Cannot locate service to invoke (location or invoke name missing)");

        // get the classloader to use
        ClassLoader classLoader = null;

        if (dctx != null)
            classLoader = dctx.getClassLoader();

        // if the classLoader is null, no big deal, SimpleMethod will use the 
        // current thread's ClassLoader by default if null passed in

        try {
            return SimpleMethod.runSimpleService(this.getLocation(modelService), modelService.invoke, dctx, context, classLoader);
        } catch (MiniLangException e) {
            throw new GenericServiceException("Error running simple method [" + modelService.invoke +
                    "] in XML file [" + modelService.location + "]: ", e);
        }
    }
}
