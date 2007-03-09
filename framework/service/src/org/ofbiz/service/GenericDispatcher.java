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
package org.ofbiz.service;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.Debug;

/**
 * Generic Services Local Dispatcher
 */
public class GenericDispatcher extends GenericAbstractDispatcher {

    public static final String module = GenericDispatcher.class.getName();
    
    protected static Map dispatcherCache = FastMap.newInstance();
    
    public static LocalDispatcher getLocalDispatcher(String dispatcherName, GenericDelegator delegator) {
        return getLocalDispatcher(dispatcherName, delegator, null, null);
    }

    public static LocalDispatcher getLocalDispatcher(String dispatcherName, GenericDelegator delegator, ClassLoader loader) {
        return getLocalDispatcher(dispatcherName, delegator, loader, null);
    }
    
    public static LocalDispatcher getLocalDispatcher(String dispatcherName, GenericDelegator delegator, ClassLoader loader, ServiceDispatcher serviceDispatcher) {
        if (dispatcherName == null) {
            dispatcherName = "default";
            Debug.logWarning("Got a getGenericDelegator call with a null dispatcherName, assuming default for the name.", module);
        }
        LocalDispatcher dispatcher = (LocalDispatcher) dispatcherCache.get(dispatcherName);

        if (dispatcher == null) {
            synchronized (GenericDelegator.class) {
                // must check if null again as one of the blocked threads can still enter
                dispatcher = (GenericDispatcher) dispatcherCache.get(dispatcherName);
                if (dispatcher == null) {
                    if (Debug.infoOn()) Debug.logInfo("Creating new dispatcher [" + dispatcherName + "] (" + Thread.currentThread().getName() + ")", module);
                    //Debug.logInfo(new Exception(), "Showing stack where new dispatcher is being created...", module);
                    
                    if (delegator == null && serviceDispatcher != null) {
                        delegator = serviceDispatcher.getDelegator();
                    }
                    
                    if (loader == null) {
                        loader = GenericDispatcher.class.getClassLoader();
                    }
                    
                    ServiceDispatcher sd = serviceDispatcher != null? serviceDispatcher : ServiceDispatcher.getInstance(dispatcherName, delegator);
                    LocalDispatcher thisDispatcher = null;
                    if (sd != null) {
                        dispatcher = sd.getLocalDispatcher(dispatcherName);
                    }
                    if (thisDispatcher == null) {
                        dispatcher = new GenericDispatcher(dispatcherName, delegator, loader, sd);
                    }

                    if (dispatcher != null) {
                        dispatcherCache.put(dispatcherName, dispatcher);
                    } else {
                        Debug.logError("Could not create dispatcher with name " + dispatcherName + ", constructor failed (got null value) not sure why/how.", module);
                    }
                }
            }
        }
        return dispatcher;
    }

    /** special method to obtain a new 'unique' reference with a variation on parameters */
    public static LocalDispatcher newInstance(String name, GenericDelegator delegator, boolean enableJM, boolean enableJMS, boolean enableSvcs) throws GenericServiceException {
        ServiceDispatcher sd = new ServiceDispatcher(delegator, enableJM, enableJMS, enableSvcs);
        ClassLoader loader = null;
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (SecurityException e) {
            loader = GenericDispatcher.class.getClassLoader();
        }
        return new GenericDispatcher(name, delegator, loader, sd);        
    }

    protected GenericDispatcher() {}

    protected GenericDispatcher(String name, GenericDelegator delegator, ClassLoader loader, ServiceDispatcher serviceDispatcher) {
        if (serviceDispatcher != null) {
            this.dispatcher = serviceDispatcher;
        }
        if (loader == null) {
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                loader = this.getClass().getClassLoader();
            }
        }
        DispatchContext dc = new DispatchContext(name, null, loader, null);
        init(name, delegator, dc);
    }

    protected void init(String name, GenericDelegator delegator, DispatchContext ctx) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("The name of a LocalDispatcher cannot be a null or empty String");

        this.name = name;
        this.ctx = ctx;
        this.dispatcher = ServiceDispatcher.getInstance(name, ctx, delegator);

        ctx.setDispatcher(this);
        ctx.loadReaders();
        if (Debug.infoOn()) Debug.logInfo("[LocalDispatcher] : Created Dispatcher for: " + name, module);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSync(java.lang.String, java.util.Map)
     */
    public Map runSync(String serviceName, Map context) throws ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        return dispatcher.runSync(this.name, service, context);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSync(java.lang.String, java.util.Map, int, boolean)
     */
    public Map runSync(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        // clone the model service for updates
        ModelService cloned = new ModelService(service);
        cloned.requireNewTransaction = requireNewTransaction;
        if (transactionTimeout != -1) {
            cloned.transactionTimeout = transactionTimeout;
        }
        return dispatcher.runSync(this.name, cloned, context);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSyncIgnore(java.lang.String, java.util.Map)
     */
    public void runSyncIgnore(String serviceName, Map context) throws GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        dispatcher.runSyncIgnore(this.name, service, context);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSyncIgnore(java.lang.String, java.util.Map)
     */
    public void runSyncIgnore(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        // clone the model service for updates
        ModelService cloned = new ModelService(service);
        cloned.requireNewTransaction = requireNewTransaction;
        if (transactionTimeout != -1) {
            cloned.transactionTimeout = transactionTimeout;
        }
        dispatcher.runSyncIgnore(this.name, cloned, context);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, org.ofbiz.service.GenericRequester, boolean, int, boolean)
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        // clone the model service for updates
        ModelService cloned = new ModelService(service);
        cloned.requireNewTransaction = requireNewTransaction;
        if (transactionTimeout != -1) {
            cloned.transactionTimeout = transactionTimeout;
        }
        dispatcher.runAsync(this.name, cloned, context, requester, persist);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, org.ofbiz.service.GenericRequester, boolean)
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        dispatcher.runAsync(this.name, service, context, requester, persist);
    }
   
    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, org.ofbiz.service.GenericRequester)
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, context, requester, true);
    }
    
    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, boolean)
     */
    public void runAsync(String serviceName, Map context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        dispatcher.runAsync(this.name, service, context, persist);
    }
   
    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map)
     */
    public void runAsync(String serviceName, Map context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, context, true);
    }
  
    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsyncWait(java.lang.String, java.util.Map, boolean)
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        GenericResultWaiter waiter = new GenericResultWaiter();
        this.runAsync(serviceName, context, waiter, persist);
        return waiter;
    }
 
    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsyncWait(java.lang.String, java.util.Map)
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        return runAsyncWait(serviceName, context, true);
    }  
}

