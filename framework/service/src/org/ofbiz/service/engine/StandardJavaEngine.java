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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.base.util.Debug;

/**
 * Standard Java Static Method Service Engine
 */
public final class StandardJavaEngine extends GenericAsyncEngine {

    public static final String module = StandardJavaEngine.class.getName();

    public StandardJavaEngine(ServiceDispatcher dispatcher) {
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

    // Invoke the static java method service.
    private Object serviceInvoker(String localName, ModelService modelService, Map context) throws GenericServiceException {
        // static java service methods should be: public Map methodName(DispatchContext dctx, Map context)
        DispatchContext dctx = dispatcher.getLocalContext(localName);

        if (modelService == null)
            Debug.logError("ERROR: Null Model Service.", module);
        if (dctx == null)
            Debug.logError("ERROR: Null DispatchContext.", module);
        if (context == null)
            Debug.logError("ERROR: Null Service Context.", module);

        Class[] paramTypes = new Class[] {DispatchContext.class, Map.class};
        Object[] params = new Object[] {dctx, context};
        Object result = null;

        // check the package and method names
        if (modelService.location == null || modelService.invoke == null)
            throw new GenericServiceException("Cannot locate service to invoke (location or invoke name missing)");

        // get the classloader to use
        ClassLoader cl = null;

        if (dctx == null)
            cl = this.getClass().getClassLoader();
        else
            cl = dctx.getClassLoader();

        try {
            Class c = cl.loadClass(this.getLocation(modelService));
            Method m = c.getMethod(modelService.invoke, paramTypes);
            result = m.invoke(null, params);
        } catch (ClassNotFoundException cnfe) {
            throw new GenericServiceException("Cannot find service location", cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new GenericServiceException("Service method does not exist", nsme);
        } catch (SecurityException se) {
            throw new GenericServiceException("Access denied", se);
        } catch (IllegalAccessException iae) {
            throw new GenericServiceException("Method not accessible", iae);
        } catch (IllegalArgumentException iarge) {
            throw new GenericServiceException("Invalid parameter match", iarge);
        } catch (InvocationTargetException ite) {
            throw new GenericServiceException("Service target threw an unexpected exception", ite.getTargetException());
        } catch (NullPointerException npe) {
            throw new GenericServiceException("Specified object is null", npe);
        } catch (ExceptionInInitializerError eie) {
            throw new GenericServiceException("Initialization failed", eie);
        } catch (Throwable th) {
            throw new GenericServiceException("Error or nknown exception", th);
        }

        return result;
    }
}

