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
import java.util.Set;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;

/**
 * Generic Services Local Dispatcher
 */
public class GenericDispatcher extends GenericAbstractDispatcher {

    public static final String module = GenericDispatcher.class.getName();

    protected static boolean ecasDisabled = false;
    protected static Map<String, LocalDispatcher> dispatcherCache = FastMap.newInstance();

    public static LocalDispatcher getLocalDispatcher(String dispatcherName, Delegator delegator) {
        if (dispatcherName == null) {
            dispatcherName = delegator.getDelegatorName();
            Debug.logWarning("Got a getGenericDispatcher call with a null dispatcherName, assuming default for the name.", module);
        }

        if (UtilValidate.isNotEmpty(delegator.getDelegatorTenantId())) {
            dispatcherName += "#" + delegator.getDelegatorTenantId();
        }

        LocalDispatcher dispatcher = dispatcherCache.get(dispatcherName);

        if (dispatcher == null) {
            synchronized (GenericDispatcher.class) {
                // must check if null again as one of the blocked threads can still enter
                dispatcher = dispatcherCache.get(dispatcherName);
                if (dispatcher == null) {
                    if (Debug.infoOn()) Debug.logInfo("Creating new dispatcher [" + dispatcherName + "] (" + Thread.currentThread().getName() + ")", module);
                    // attempts to retrieve an already registered DispatchContext with the name dispatcherName
                    dispatcher = ServiceDispatcher.getLocalDispatcher(dispatcherName, delegator);
                    // if not found then create a new GenericDispatcher object; the constructor will also register a new DispatchContext in the ServiceDispatcher with name "dispatcherName"
                    if (dispatcher == null) {
                        dispatcher = new GenericDispatcher(dispatcherName, delegator);
                    }

                    dispatcherCache.put(dispatcherName, dispatcher);
                }
            }
        }
        return dispatcher;
    }

    public static Set<String> getAllDispatcherNames() {
        return dispatcherCache.keySet();
    }

    protected GenericDispatcher() {}

    protected GenericDispatcher(String name, Delegator delegator) {
        if (UtilValidate.isEmpty(name)) {
            throw new IllegalArgumentException("The name of a LocalDispatcher cannot be a null or empty String");
        }
        ClassLoader loader;
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (SecurityException e) {
            loader = this.getClass().getClassLoader();
        }

        this.name = name;
        this.dispatcher = ServiceDispatcher.getInstance(delegator);
        DispatchContext ctx = new DispatchContext(name, loader, this);
        this.dispatcher.register(ctx);
        this.ctx = ctx;
        if (Debug.verboseOn()) Debug.logVerbose("[LocalDispatcher] : Created Dispatcher for: " + name, module);
    }

    public void disableEcas() {
        ecasDisabled = true;
    }

    public void enableEcas() {
        ecasDisabled = false;
    }

    public boolean isEcasDisabled() {
        return ecasDisabled;
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSync(java.lang.String, java.util.Map)
     */
    public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context) throws ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        return dispatcher.runSync(this.name, service, context);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSync(java.lang.String, java.util.Map, int, boolean)
     */
    public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        // clone the model service for updates
        ModelService cloned = new ModelService(service);
        cloned.requireNewTransaction = requireNewTransaction;
        if (transactionTimeout != -1) {
            cloned.transactionTimeout = transactionTimeout;
        }
        return dispatcher.runSync(this.name, cloned, context);
    }

    public Map<String, Object> runSync(String serviceName, int transactionTimeout, boolean requireNewTransaction, Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        return runSync(serviceName, ServiceUtil.makeContext(context), transactionTimeout, requireNewTransaction);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSyncIgnore(java.lang.String, java.util.Map)
     */
    public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        dispatcher.runSyncIgnore(this.name, service, context);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runSyncIgnore(java.lang.String, java.util.Map)
     */
    public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        // clone the model service for updates
        ModelService cloned = new ModelService(service);
        cloned.requireNewTransaction = requireNewTransaction;
        if (transactionTimeout != -1) {
            cloned.transactionTimeout = transactionTimeout;
        }
        dispatcher.runSyncIgnore(this.name, cloned, context);
    }

    public void runSyncIgnore(String serviceName, int transactionTimeout, boolean requireNewTransaction, Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runSyncIgnore(serviceName, ServiceUtil.makeContext(context), transactionTimeout, requireNewTransaction);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, org.ofbiz.service.GenericRequester, boolean, int, boolean)
     */
    public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester, boolean persist, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        // clone the model service for updates
        ModelService cloned = new ModelService(service);
        cloned.requireNewTransaction = requireNewTransaction;
        if (transactionTimeout != -1) {
            cloned.transactionTimeout = transactionTimeout;
        }
        dispatcher.runAsync(this.name, cloned, context, requester, persist);
    }

    public void runAsync(String serviceName, GenericRequester requester, boolean persist, int transactionTimeout, boolean requireNewTransaction, Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, ServiceUtil.makeContext(context), requester, persist, transactionTimeout, requireNewTransaction);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, org.ofbiz.service.GenericRequester, boolean)
     */
    public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        dispatcher.runAsync(this.name, service, context, requester, persist);
    }

    public void runAsync(String serviceName, GenericRequester requester, boolean persist, Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, ServiceUtil.makeContext(context), requester, persist);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, org.ofbiz.service.GenericRequester)
     */
    public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, context, requester, true);
    }

    public void runAsync(String serviceName, GenericRequester requester, Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, ServiceUtil.makeContext(context), requester);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map, boolean)
     */
    public void runAsync(String serviceName, Map<String, ? extends Object> context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        ModelService service = ctx.getModelService(serviceName);
        dispatcher.runAsync(this.name, service, context, persist);
    }

    public void runAsync(String serviceName, boolean persist, Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, ServiceUtil.makeContext(context), persist);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsync(java.lang.String, java.util.Map)
     */
    public void runAsync(String serviceName, Map<String, ? extends Object> context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runAsync(serviceName, context, true);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsyncWait(java.lang.String, java.util.Map, boolean)
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map<String, ? extends Object> context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        GenericResultWaiter waiter = new GenericResultWaiter();
        this.runAsync(serviceName, context, waiter, persist);
        return waiter;
    }

    public GenericResultWaiter runAsyncWait(String serviceName, boolean persist, Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        return runAsyncWait(serviceName, ServiceUtil.makeContext(context), persist);
    }

    /**
     * @see org.ofbiz.service.LocalDispatcher#runAsyncWait(java.lang.String, java.util.Map)
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map<String, ? extends Object> context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        return runAsyncWait(serviceName, context, true);
    }
}

