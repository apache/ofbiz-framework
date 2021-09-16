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
package org.apache.ofbiz.service;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;

/**
 * A default {@link LocalDispatcherFactory} implementation.
 */
public class GenericDispatcherFactory implements LocalDispatcherFactory {

    private static final String MODULE = GenericDispatcherFactory.class.getName();

    private static boolean ecasDisabled = false;

    @Override
    public LocalDispatcher createLocalDispatcher(String name, Delegator delegator) {
        if (UtilValidate.isEmpty(name)) {
            throw new IllegalArgumentException("The name of a LocalDispatcher cannot be a null or empty String");
        }
        // attempts to retrieve an already registered DispatchContext with the name "name"
        LocalDispatcher dispatcher = ServiceDispatcher.getLocalDispatcher(name, delegator);
        // if not found then create a new GenericDispatcher object; the constructor will also register a new DispatchContext in the
        // ServiceDispatcher with name "dispatcherName"
        if (dispatcher == null) {
            dispatcher = new GenericDispatcher(name, delegator);
        }
        return dispatcher;
    }

    // The default LocalDispatcher implementation.
    private static final class GenericDispatcher extends GenericAbstractDispatcher {
        private GenericDispatcher(String name, Delegator delegator) {
            ClassLoader loader;
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                loader = this.getClass().getClassLoader();
            }
            this.setName(name);
            this.setDispatcher(ServiceDispatcher.getInstance(delegator));
            /*
             * FIXME: "this" reference escape. DispatchContext constructor uses
             * this object before it is fully constructed.
             */
            DispatchContext ctx = new DispatchContext(name, loader, this);
            this.getDispatcher().register(ctx);
            this.setCtx(ctx);
            if (Debug.verboseOn()) {
                Debug.logVerbose("[GenericDispatcher] : Created Dispatcher for: " + name, MODULE);
            }
        }

        @Override
        public void disableEcas() {
            ecasDisabled = true;
        }

        @Override
        public void enableEcas() {
            ecasDisabled = false;
        }

        @Override
        public boolean isEcasDisabled() {
            return ecasDisabled;
        }

        @Override
        public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context)
                throws ServiceValidationException, GenericServiceException {
            ModelService service = getCtx().getModelService(serviceName);
            return getDispatcher().runSync(this.getName(), service, context);
        }

        @Override
        public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context, int transactionTimeout,
                                           boolean requireNewTransaction)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getCtx().getModelService(serviceName);
            // clone the model service for updates
            ModelService cloned = new ModelService(service);
            cloned.setRequireNewTransaction(requireNewTransaction);
            if (requireNewTransaction) {
                cloned.setUseTransaction(true);
            }
            if (transactionTimeout != -1) {
                cloned.setTransactionTimeout(transactionTimeout);
            }
            return getDispatcher().runSync(this.getName(), cloned, context);
        }

        @Override
        public Map<String, Object> runSync(String serviceName, int transactionTimeout, boolean requireNewTransaction, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            return runSync(serviceName, ServiceUtil.makeContext(context), transactionTimeout, requireNewTransaction);
        }

        @Override
        public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException {
            ModelService service = getCtx().getModelService(serviceName);
            getDispatcher().runSyncIgnore(this.getName(), service, context);
        }

        @Override
        public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context, int transactionTimeout, boolean requireNewTransaction)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getCtx().getModelService(serviceName);
            // clone the model service for updates
            ModelService cloned = new ModelService(service);
            cloned.setRequireNewTransaction(requireNewTransaction);
            if (requireNewTransaction) {
                cloned.setUseTransaction(true);
            }
            if (transactionTimeout != -1) {
                cloned.setTransactionTimeout(transactionTimeout);
            }
            getDispatcher().runSyncIgnore(this.getName(), cloned, context);
        }

        @Override
        public void runSyncIgnore(String serviceName, int transactionTimeout, boolean requireNewTransaction, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runSyncIgnore(serviceName, ServiceUtil.makeContext(context), transactionTimeout, requireNewTransaction);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester, boolean persist,
                             int transactionTimeout, boolean requireNewTransaction)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getCtx().getModelService(serviceName);
            // clone the model service for updates
            ModelService cloned = new ModelService(service);
            cloned.setRequireNewTransaction(requireNewTransaction);
            if (requireNewTransaction) {
                cloned.setUseTransaction(true);
            }
            if (transactionTimeout != -1) {
                cloned.setTransactionTimeout(transactionTimeout);
            }
            getDispatcher().runAsync(this.getName(), cloned, context, requester, persist);
        }

        @Override
        public void runAsync(String serviceName, GenericRequester requester, boolean persist, int transactionTimeout,
                             boolean requireNewTransaction, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runAsync(serviceName, ServiceUtil.makeContext(context), requester, persist, transactionTimeout, requireNewTransaction);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester, boolean persist)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getCtx().getModelService(serviceName);
            getDispatcher().runAsync(this.getName(), service, context, requester, persist);
        }

        @Override
        public void runAsync(String serviceName, GenericRequester requester, boolean persist, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runAsync(serviceName, ServiceUtil.makeContext(context), requester, persist);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runAsync(serviceName, context, requester, true);
        }

        @Override
        public void runAsync(String serviceName, GenericRequester requester, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runAsync(serviceName, ServiceUtil.makeContext(context), requester);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context, boolean persist)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getCtx().getModelService(serviceName);
            getDispatcher().runAsync(this.getName(), service, context, persist);
        }

        @Override
        public void runAsync(String serviceName, boolean persist, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runAsync(serviceName, ServiceUtil.makeContext(context), persist);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runAsync(serviceName, context, true);
        }

        @Override
        public GenericResultWaiter runAsyncWait(String serviceName, Map<String, ? extends Object> context, boolean persist)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            GenericResultWaiter waiter = new GenericResultWaiter();
            this.runAsync(serviceName, context, waiter, persist);
            return waiter;
        }

        @Override
        public GenericResultWaiter runAsyncWait(String serviceName, boolean persist, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            return runAsyncWait(serviceName, ServiceUtil.makeContext(context), persist);
        }

        @Override
        public GenericResultWaiter runAsyncWait(String serviceName, Map<String, ? extends Object> context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            return runAsyncWait(serviceName, context, true);
        }
    }

}
