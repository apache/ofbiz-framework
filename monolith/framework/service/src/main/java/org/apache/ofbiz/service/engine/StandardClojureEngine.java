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
package org.apache.ofbiz.service.engine;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Clojure service engine. Enables OFBiz services written in Clojure.
 */
public class StandardClojureEngine extends GenericAsyncEngine {

    private static final String MODULE = StandardClojureEngine.class.getName();

    public StandardClojureEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
        Debug.logInfo("Created Clojure engine.", MODULE);
    }

    /**
     * Load Clojure ns and call service function.
     * <p>
     * See https://clojure.github.io/clojure/javadoc/clojure/java/api/Clojure.html
     *
     * @param ns      Clojure namespace to load
     * @param fn      Clojure function to call
     * @param dctx    OFBiz dispatch context
     * @param context OFBiz context - input parameters
     * @return
     * @throws Exception
     */
    public static Object callClojure(String ns, String fn, DispatchContext dctx, Map<String, Object> context) throws Exception {
        Debug.logInfo("Call %s/%s ", MODULE, ns, fn);
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read(ns));
        return Clojure.var(ns, fn).invoke(dctx, context);
    }

    @Override
    public void runSyncIgnore(String localName, ModelService modelService,
                              Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService,
                                       Map<String, Object> context) throws GenericServiceException {

        Object result = serviceInvoker(localName, modelService, context);

        if (result == null || !(result instanceof Map<?, ?>)) {
            throw new GenericServiceException(
                    "Service [" + modelService.getName() + "] did not return a Map object");
        }
        return UtilGenerics.cast(result);
    }

    private Object serviceInvoker(String localName, ModelService modelService,
                                  Map<String, Object> context) throws GenericServiceException {
        DispatchContext dctx = getDispatcher().getLocalContext(localName);
        if (modelService == null) {
            Debug.logError("ERROR: Null Model Service.", MODULE);
        }
        if (dctx == null) {
            Debug.logError("ERROR: Null DispatchContext.", MODULE);
        }
        if (context == null) {
            Debug.logError("ERROR: Null Service Context.", MODULE);
        }
        Object result = null;

        // check the namespace and function names
        if (modelService.getLocation() == null || modelService.getInvoke() == null) {
            throw new GenericServiceException("Service [" + modelService.getName()
                    + "] is missing location and/or invoke values which are required for execution.");
        }

        try {
            String ns = this.getLocation(modelService);
            String fn = modelService.getInvoke();
            result = callClojure(ns, fn, dctx, context);
        } catch (ClassNotFoundException cnfe) {
            throw new GenericServiceException(
                    "Cannot find service [" + modelService.getName() + "] location class", cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new GenericServiceException("Service [" + modelService.getName()
                    + "] specified Java method (invoke attribute) does not exist",
                    nsme);
        } catch (SecurityException se) {
            throw new GenericServiceException("Service [" + modelService.getName() + "] Access denied",
                    se);
        } catch (IllegalAccessException iae) {
            throw new GenericServiceException(
                    "Service [" + modelService.getName() + "] Method not accessible", iae);
        } catch (IllegalArgumentException iarge) {
            throw new GenericServiceException(
                    "Service [" + modelService.getName() + "] Invalid parameter match", iarge);
        } catch (InvocationTargetException ite) {
            throw new GenericServiceException(
                    "Service [" + modelService.getName() + "] target threw an unexpected exception",
                    ite.getTargetException());
        } catch (NullPointerException npe) {
            throw new GenericServiceException(
                    "Service [" + modelService.getName() + "] ran into an unexpected null object", npe);
        } catch (ExceptionInInitializerError eie) {
            throw new GenericServiceException(
                    "Service [" + modelService.getName() + "] Initialization failed", eie);
        } catch (Throwable th) {
            throw new GenericServiceException(
                    "Service [" + modelService.getName() + "] Error or unknown exception", th);
        }

        return result;
    }
}
