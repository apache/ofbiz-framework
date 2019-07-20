/*
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
 */
package org.apache.ofbiz.service.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;

/**
 * Standard Java Static Method Service Engine
 */
public final class StandardJavaEngine extends GenericAsyncEngine {

    public static final String module = StandardJavaEngine.class.getName();

    public StandardJavaEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Object result = serviceInvoker(localName, modelService, context);

        if (result == null || !(result instanceof Map<?, ?>)) {
            throw new GenericServiceException("Service [" + modelService.name + "] did not return a Map object");
        }
        return UtilGenerics.cast(result);
    }

    // Invoke the static java method service.
    private Object serviceInvoker(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        // static java service methods should be: public Map<String, Object> methodName(DispatchContext dctx, Map<String, Object> context)
        DispatchContext dctx = dispatcher.getLocalContext(localName);

        if (modelService == null) {
            Debug.logError("ERROR: Null Model Service.", module);
        }
        if (dctx == null) {
            Debug.logError("ERROR: Null DispatchContext.", module);
        }
        if (context == null) {
            Debug.logError("ERROR: Null Service Context.", module);
        }

        Object result = null;

        // check the package and method names
        if (modelService.location == null || modelService.invoke == null) {
            throw new GenericServiceException("Service [" + modelService.name + "] is missing location and/or invoke values which are required for execution.");
        }

        // get the classloader to use
        ClassLoader cl = null;

        if (dctx == null) {
            cl = this.getClass().getClassLoader();
        } else {
            cl = dctx.getClassLoader();
        }

        try {
            Class<?> c = cl.loadClass(this.getLocation(modelService));
            Method m = c.getMethod(modelService.invoke, DispatchContext.class, Map.class);
            if (Modifier.isStatic(m.getModifiers())) {
                result = m.invoke(null, dctx, context);
            } else {
                result = m.invoke(c.getDeclaredConstructor().newInstance(), dctx, context);
            }
        } catch (ClassNotFoundException cnfe) {
            throw new GenericServiceException("Cannot find service [" + modelService.name + "] location class", cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new GenericServiceException("Service [" + modelService.name + "] specified Java method (invoke attribute) does not exist", nsme);
        } catch (SecurityException se) {
            throw new GenericServiceException("Service [" + modelService.name + "] Access denied", se);
        } catch (IllegalAccessException iae) {
            throw new GenericServiceException("Service [" + modelService.name + "] Method not accessible", iae);
        } catch (IllegalArgumentException iarge) {
            throw new GenericServiceException("Service [" + modelService.name + "] Invalid parameter match", iarge);
        } catch (InvocationTargetException ite) {
            throw new GenericServiceException("Service [" + modelService.name + "] target threw an unexpected exception", ite.getTargetException());
        } catch (NullPointerException npe) {
            throw new GenericServiceException("Service [" + modelService.name + "] ran into an unexpected null object", npe);
        } catch (ExceptionInInitializerError eie) {
            throw new GenericServiceException("Service [" + modelService.name + "] Initialization failed", eie);
        } catch (Throwable th) {
            throw new GenericServiceException("Service [" + modelService.name + "] Error or unknown exception", th);
        }

        return result;
    }
}

