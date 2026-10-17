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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionFactoryLoader;
import org.apache.ofbiz.entity.transaction.TransactionUtil;

/**
 * This class is used to execute services when a transaction is either
 * committed or rolled back.  It should generally be accessed via
 * LocalDispatcher's addCommitService and addRollbackService methods
 * or by using the service ECA event attribute values global-commit,
 * global-rollback or global-commit-post-run
 *
 */
public class ServiceSynchronization implements Synchronization {

    private static final String MODULE = ServiceSynchronization.class.getName();

    private static Map<Transaction, ServiceSynchronization> syncingleton = new WeakHashMap<>();
    private List<ServiceExecution> services = new ArrayList<>();

    public static void registerCommitService(DispatchContext dctx, String serviceName, String runAsUser, Map<String, ? extends Object> context,
                                             boolean async, boolean persist) throws GenericServiceException {
        ServiceSynchronization sync = ServiceSynchronization.getInstance();
        sync.services.add(new ServiceExecution(dctx, serviceName, runAsUser, context, async, persist, false));
    }

    public static void registerRollbackService(DispatchContext dctx, String serviceName, String runAsUser, Map<String, ? extends Object> context,
                                               boolean async, boolean persist) throws GenericServiceException {
        ServiceSynchronization sync = ServiceSynchronization.getInstance();
        sync.services.add(new ServiceExecution(dctx, serviceName, runAsUser, context, async, persist, true));
    }

    protected static ServiceSynchronization getInstance() throws GenericServiceException {
        ServiceSynchronization sync = null;
        try {
            Transaction transaction = TransactionFactoryLoader.getInstance().getTransactionManager().getTransaction();
            synchronized (transaction) {
                sync = syncingleton.get(transaction);
                if (sync == null) {
                    sync = new ServiceSynchronization();
                    transaction.registerSynchronization(sync);
                    syncingleton.put(transaction, sync);
                }
            }
        } catch (SystemException | IllegalStateException | RollbackException e) {
            throw new GenericServiceException(e.getMessage(), e);
        }
        return sync;
    }

    @Override
    public void afterCompletion(int status) {
        long start = System.currentTimeMillis();
        if (Debug.verboseOn()) {
            Debug.logVerbose("Running " + this.services.size() + " services on commit/rollback", MODULE);
        }
        for (ServiceExecution serviceExec : this.services) {
            serviceExec.runService(status);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Completed service [" + serviceExec.serviceName + "] async [" + serviceExec.async
                        + "] persisted [" + serviceExec.persist + "]", MODULE);
            }
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Commit/rollback services complete in "
                    + (System.currentTimeMillis() - start) + "ms", MODULE);
        }
    }

    @Override
    public void beforeCompletion() {

    }

    static class ServiceExecution {
        private DispatchContext dctx = null;
        private String serviceName;
        private String runAsUser = null;
        private Map<String, ? extends Object> context = null;
        private boolean rollback = false;
        private boolean persist = true;
        private boolean async = false;

        ServiceExecution(DispatchContext dctx, String serviceName, String runAsUser, Map<String, ? extends Object> context, boolean async,
                         boolean persist, boolean rollback) {
            this.dctx = dctx;
            this.serviceName = serviceName;
            this.runAsUser = runAsUser;
            this.context = context;
            this.async = async;
            this.persist = persist;
            this.rollback = rollback;
        }

        protected void runService(int status) {
            if ((status == Status.STATUS_COMMITTED && !rollback) || (status == Status.STATUS_ROLLEDBACK && rollback)) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        String msgPrefix = null;
                        if (rollback) {
                            msgPrefix = "[Rollback] ";
                        } else {
                            msgPrefix = "[Commit] ";
                        }

                        boolean beganTx;
                        try {
                            // begin the new tx
                            beganTx = TransactionUtil.begin();
                            // configure and run the service
                            try {
                                // obtain the model and get the valid context
                                ModelService model = dctx.getModelService(serviceName);
                                Map<String, Object> thisContext;
                                if (model.isValidate()) {
                                    thisContext = model.makeValid(context, ModelService.IN_PARAM);
                                } else {
                                    thisContext = new HashMap<>();
                                    thisContext.putAll(context);
                                }

                                // set the userLogin object
                                thisContext.put("userLogin", ServiceUtil.getUserLogin(dctx, thisContext, runAsUser));
                                if (async) {
                                    Debug.logInfo(msgPrefix + "Invoking [" + serviceName + "] via runAsync", MODULE);
                                    dctx.getDispatcher().runAsync(serviceName, thisContext, persist);
                                } else {
                                    Debug.logInfo(msgPrefix + "Invoking [" + serviceName + "] via runSyncIgnore", MODULE);
                                    dctx.getDispatcher().runSyncIgnore(serviceName, thisContext);
                                }
                            } catch (Throwable t) {
                                Debug.logError(t, "Problem calling " + msgPrefix + "service : " + serviceName + " / " + context, MODULE);
                                try {
                                    TransactionUtil.rollback(beganTx, t.getMessage(), t);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, MODULE);
                                }

                            } finally {
                                // commit the transaction
                                try {
                                    TransactionUtil.commit(beganTx);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, MODULE);
                                }
                            }
                        } catch (GenericTransactionException e) {
                            Debug.logError(e, MODULE);
                        }
                    }
                };
                thread.start();
            }
        }
    }
}
