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
package org.ofbiz.minilang;

import java.util.Map;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.engine.GenericAsyncEngine;

/**
 * Standard Java Static Method Service Engine
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
