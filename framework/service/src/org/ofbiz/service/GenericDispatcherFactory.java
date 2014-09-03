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
import java.util.TreeSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityConfException;
import org.ofbiz.entity.config.model.DelegatorElement;
import org.ofbiz.entity.config.model.EntityConfig;

/**
 * A default {@link LocalDispatcherFactory} implementation.
 */
public class GenericDispatcherFactory implements LocalDispatcherFactory {

    public static final String module = GenericDispatcherFactory.class.getName();
    protected static boolean ecasDisabled = false;

    @Override
    public LocalDispatcher createLocalDispatcher(String name, Delegator delegator) {
        if (UtilValidate.isEmpty(name)) {
            throw new IllegalArgumentException("The name of a LocalDispatcher cannot be a null or empty String");
        }
        // attempts to retrieve an already registered LocalDispatcher with the name "name"
        LocalDispatcher dispatcher = ServiceDispatcher.getLocalDispatcher(name, delegator);
        // if not found then create a new GenericDispatcher object
        if (dispatcher == null) {
            ServiceDispatcher globalDispatcher = ServiceDispatcher.getInstance(delegator);
            String modelName = name;
            if (delegator != null) {
                DelegatorElement delegatorInfo = null;
                try {
                    delegatorInfo = EntityConfig.getInstance().getDelegator(delegator.getDelegatorBaseName());
                } catch (GenericEntityConfException e) {
                    Debug.logWarning(e, "Exception thrown while getting delegator config: ", module);
                }
                if (delegatorInfo != null) {
                    modelName = delegatorInfo.getEntityModelReader();
                }
            }
            Map<String, ModelService> serviceMap = globalDispatcher.getGlobalServiceMap(modelName);
            dispatcher = globalDispatcher.register(new GenericDispatcher(name, globalDispatcher, serviceMap));
        }
        return dispatcher;
    }

    // The default LocalDispatcher implementation.
    private class GenericDispatcher extends GenericAbstractDispatcher {

        private final ClassLoader loader;
        private final Map<String, ModelService> serviceMap;

        private GenericDispatcher(String name, ServiceDispatcher globalDispatcher, Map<String, ModelService> serviceMap) {
            ClassLoader loader;
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                loader = this.getClass().getClassLoader();
            }
            this.loader = loader;
            this.name = name;
            this.globalDispatcher = globalDispatcher;
            this.serviceMap = serviceMap;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created Dispatcher for: " + name, module);
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
            ModelService service = getModelService(serviceName);
            return globalDispatcher.runSync(this.name, service, context);
        }

        @Override
        public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context, int transactionTimeout,
                boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getModelService(serviceName);
            // clone the model service for updates
            ModelService cloned = new ModelService(service);
            cloned.requireNewTransaction = requireNewTransaction;
            if (requireNewTransaction) {
                cloned.useTransaction = true;
            }
            if (transactionTimeout != -1) {
                cloned.transactionTimeout = transactionTimeout;
            }
            return globalDispatcher.runSync(this.name, cloned, context);
        }

        @Override
        public Map<String, Object> runSync(String serviceName, int transactionTimeout, boolean requireNewTransaction,
                Object... context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            return runSync(serviceName, ServiceUtil.makeContext(context), transactionTimeout, requireNewTransaction);
        }

        @Override
        public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException {
            ModelService service = getModelService(serviceName);
            globalDispatcher.runSyncIgnore(this.name, service, context);
        }

        @Override
        public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context, int transactionTimeout,
                boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getModelService(serviceName);
            // clone the model service for updates
            ModelService cloned = new ModelService(service);
            cloned.requireNewTransaction = requireNewTransaction;
            if (requireNewTransaction) {
                cloned.useTransaction = true;
            }
            if (transactionTimeout != -1) {
                cloned.transactionTimeout = transactionTimeout;
            }
            globalDispatcher.runSyncIgnore(this.name, cloned, context);
        }

        @Override
        public void runSyncIgnore(String serviceName, int transactionTimeout, boolean requireNewTransaction, Object... context)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            runSyncIgnore(serviceName, ServiceUtil.makeContext(context), transactionTimeout, requireNewTransaction);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester,
                boolean persist, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException,
                ServiceValidationException, GenericServiceException {
            ModelService service = getModelService(serviceName);
            // clone the model service for updates
            ModelService cloned = new ModelService(service);
            cloned.requireNewTransaction = requireNewTransaction;
            if (requireNewTransaction) {
                cloned.useTransaction = true;
            }
            if (transactionTimeout != -1) {
                cloned.transactionTimeout = transactionTimeout;
            }
            globalDispatcher.runAsync(this.name, cloned, context, requester, persist);
        }

        @Override
        public void runAsync(String serviceName, GenericRequester requester, boolean persist, int transactionTimeout,
                boolean requireNewTransaction, Object... context) throws ServiceAuthException, ServiceValidationException,
                GenericServiceException {
            runAsync(serviceName, ServiceUtil.makeContext(context), requester, persist, transactionTimeout, requireNewTransaction);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester,
                boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getModelService(serviceName);
            globalDispatcher.runAsync(this.name, service, context, requester, persist);
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
        public void runAsync(String serviceName, GenericRequester requester, Object... context) throws ServiceAuthException,
                ServiceValidationException, GenericServiceException {
            runAsync(serviceName, ServiceUtil.makeContext(context), requester);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context, boolean persist)
                throws ServiceAuthException, ServiceValidationException, GenericServiceException {
            ModelService service = getModelService(serviceName);
            globalDispatcher.runAsync(this.name, service, context, persist);
        }

        @Override
        public void runAsync(String serviceName, boolean persist, Object... context) throws ServiceAuthException,
                ServiceValidationException, GenericServiceException {
            runAsync(serviceName, ServiceUtil.makeContext(context), persist);
        }

        @Override
        public void runAsync(String serviceName, Map<String, ? extends Object> context) throws ServiceAuthException,
                ServiceValidationException, GenericServiceException {
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

        @Override
        public DispatchContext getDispatchContext() {
            return new DispatchContext(this);
        }

        @Override
        public Set<String> getAllServiceNames() {
            Set<String> serviceNames = new TreeSet<String>();
            serviceNames.addAll(serviceMap.keySet());
            return serviceNames;
        }

        @Override
        public ModelService getModelService(String serviceName) throws GenericServiceException {
            ModelService modelService = serviceMap.get(serviceName);
            if (modelService == null) {
                throw new GenericServiceException("ModelService not found for name: " + serviceName);
            }
            return modelService;
        }

        @Override
        public ClassLoader getClassLoader() {
            return loader;
        }
    }
}
