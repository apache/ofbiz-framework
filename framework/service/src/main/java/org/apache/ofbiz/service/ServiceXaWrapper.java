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

import java.util.HashMap;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.GenericXaResource;
import org.apache.ofbiz.entity.transaction.TransactionUtil;

/**
 * ServiceXaWrapper - XA Resource wrapper for running services on commit() or rollback()
 * @deprecated - Use ServiceSynchronization instead (via LocalDispatcher)
 */
@Deprecated
public class ServiceXaWrapper extends GenericXaResource {

    public static final String module = ServiceXaWrapper.class.getName();
    public static final int TYPE_ROLLBACK = 600;
    public static final int TYPE_COMMIT = 500;
    public static final int MODE_ASYNC = 100;
    public static final int MODE_SYNC = 200;

    protected DispatchContext dctx = null;
    protected String rollbackService = null;
    protected String commitService = null;
    protected String runAsUser = null;
    protected Map<String, ? extends Object> rollbackContext = null;
    protected Map<String, ? extends Object> commitContext = null;
    protected boolean rollbackAsync = true;
    protected boolean rollbackAsyncPersist = true;
    protected boolean commitAsync = false;
    protected boolean commitAsyncPersist = false;

    protected ServiceXaWrapper() {}
    public ServiceXaWrapper(DispatchContext dctx) {
        this.dctx = dctx;
    }

    /**
     * Sets the service to run on commit()
     * @param serviceName Name of service to run
     * @param context Context to use when running
     */
    public void setCommitService(String serviceName, Map<String, ? extends Object> context) {
        this.setCommitService(serviceName, null, context, commitAsync, commitAsyncPersist);
    }

    /**
     * Sets the service to run on commit()
     * @param serviceName Name of service to run
     * @param context Context to use when running
     * @param async override default async behavior
     */
    public void setCommitService(String serviceName, Map<String, ? extends Object> context, boolean async, boolean persist) {
        this.setCommitService(serviceName, null, context, async, persist);
    }

    /**
     * Sets the service to run on commit()
     * @param serviceName Name of service to run
     * @param runAsUser UserLoginID to run as
     * @param context Context to use when running
     * @param async override default async behavior
     */
    public void setCommitService(String serviceName, String runAsUser, Map<String, ? extends Object> context, boolean async, boolean persist) {
        this.commitService = serviceName;
        this.runAsUser = runAsUser;
        this.commitContext = context;
        this.commitAsync = async;
        this.commitAsyncPersist = persist;
    }


    /**
     * @return The name of the service to run on commit()
     */
    public String getCommitService() {
        return this.commitService;
    }

    /**
     * @return The context used when running the commit() service
     */
    public Map<String, ? extends Object> getCommitContext() {
        return this.commitContext;
    }

    /**
     * Sets the service to run on rollback()
     * @param serviceName Name of service to run
     * @param context Context to use when running
     */
    public void setRollbackService(String serviceName, Map<String, ? extends Object> context) {
        this.setRollbackService(serviceName, context, rollbackAsync, rollbackAsyncPersist);
    }

    /**
     * Sets the service to run on rollback()
     * @param serviceName Name of service to run
     * @param context Context to use when running
     * @param async override default async behavior
     */
    public void setRollbackService(String serviceName, Map<String, ? extends Object> context, boolean async, boolean persist) {
        this.setRollbackService(serviceName, null, context, async, persist);
    }

    /**
     * Sets the service to run on rollback()
     * @param serviceName Name of service to run
     * @param runAsUser userLoginId to run the service as
     * @param context Context to use when running
     * @param async override default async behavior
     */
    public void setRollbackService(String serviceName, String runAsUser, Map<String, ? extends Object> context, boolean async, boolean persist) {
        this.rollbackService = serviceName;
        this.runAsUser = runAsUser;
        this.rollbackContext = context;
        this.rollbackAsync = async;
        this.rollbackAsyncPersist = persist;
    }

    /**
     * @return The name of the service to run on rollback()
     */
    public String getRollbackService() {
        return this.rollbackService;
    }

    /**
     * @return The context used when running the rollback() service
     */
    public Map<String, ? extends Object> getRollbackContext() {
        return this.rollbackContext;
    }

    @Override
    public void enlist() throws XAException {
        super.enlist();
        if (Debug.verboseOn()) Debug.logVerbose("Enlisted in transaction : " + this.toString(), module);
    }

    // -- XAResource Methods
    /**
     * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid xid, boolean onePhase)
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (Debug.verboseOn()) Debug.logVerbose("ServiceXaWrapper#commit() : " + onePhase + " / " + xid.toString(), module);
        // the commit listener
        if (this.active) {
            Debug.logWarning("commit() called without end()", module);
        }
        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }

        final String service = commitService;
        final Map<String, ? extends Object> context = commitContext;
        final boolean persist = commitAsyncPersist;
        final boolean async = commitAsync;

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    runService(service, context, persist, (async ? MODE_ASYNC : MODE_SYNC), TYPE_COMMIT);
                } catch (XAException e) {
                    Debug.logError(e, module);
                }
            }
        };
        thread.start();

        this.xid = null;
        this.active = false;
    }

    /**
     * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid xid)
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        if (Debug.verboseOn()) Debug.logVerbose("ServiceXaWrapper#rollback() : " + xid.toString(), module);
        // the rollback listener
        if (this.active) {
            Debug.logWarning("rollback() called without end()", module);
        }
        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }

        final String service = rollbackService;
        final Map<String, ? extends Object> context = rollbackContext;
        final boolean persist = rollbackAsyncPersist;
        final boolean async = rollbackAsync;

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    runService(service, context, persist, (async ? MODE_ASYNC : MODE_SYNC), TYPE_ROLLBACK);
                } catch (XAException e) {
                    Debug.logError(e, module);
                }
            }
        };
        thread.start();

        this.xid = null;
        this.active = false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        // overriding to log two phase commits
        if (Debug.verboseOn()) Debug.logVerbose("ServiceXaWrapper#prepare() : " + xid.toString(), module);
        int rtn;
        try {
            rtn = super.prepare(xid);
        } catch (XAException e) {
            Debug.logError(e, module);
            throw e;
        }
        if (Debug.verboseOn()) Debug.logVerbose("ServiceXaWrapper#prepare() : " + rtn + " / " + (rtn == XA_OK) , module);
        return rtn;
    }



    protected final void runService(String service, Map<String, ? extends Object> context, boolean persist, int mode, int type) throws XAException {
        // set the logging prefix
        String msgPrefix = "[XaWrapper] ";
        switch (type) {
            case TYPE_ROLLBACK:
                msgPrefix = "[Rollback] ";
                break;
            case TYPE_COMMIT:
                msgPrefix = "[Commit] ";
                break;
        }

        // if a service exists; run it
        if (UtilValidate.isNotEmpty(service)) {

            // suspend this transaction
            Transaction parentTx = null;
            boolean beganTx;

            try {
                int currentTxStatus = Status.STATUS_UNKNOWN;
                try {
                    currentTxStatus = TransactionUtil.getStatus();
                } catch (GenericTransactionException e) {
                    Debug.logWarning(e, module);
                }

                // suspend the parent tx
                if (currentTxStatus != Status.STATUS_NO_TRANSACTION) {
                    parentTx = TransactionUtil.suspend();
                }

                // begin the new tx
                beganTx = TransactionUtil.begin();

                // configure and run the service
                try {
                    // obtain the model and get the valid context
                    ModelService model = dctx.getModelService(service);
                    Map<String, Object> thisContext;
                    if (model.validate) {
                        thisContext = model.makeValid(context, ModelService.IN_PARAM);
                    } else {
                        thisContext = new HashMap<String, Object>();
                        thisContext.putAll(context);
                    }

                    // set the userLogin object
                    thisContext.put("userLogin", ServiceUtil.getUserLogin(dctx, thisContext, runAsUser));

                    // invoke based on mode
                    switch (mode) {
                        case MODE_ASYNC:
                            if (Debug.infoOn()) Debug.logInfo(msgPrefix + "Invoking [" + service + "] via runAsync", module);
                            dctx.getDispatcher().runAsync(service, thisContext, persist);
                            break;

                        case MODE_SYNC:
                            if (Debug.infoOn()) Debug.logInfo(msgPrefix + "Invoking [" + service + "] via runSyncIgnore", module);
                            dctx.getDispatcher().runSyncIgnore(service, thisContext);
                            break;
                    }
                } catch (Throwable t) {
                    Debug.logError(t, "Problem calling " + msgPrefix + "service : " + service + " / " + context, module);
                    try {
                        TransactionUtil.rollback(beganTx, t.getMessage(), t);
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, module);
                    }

                    // async calls are assumed to not effect this TX
                    if (mode != MODE_ASYNC) {
                        throw new XAException(XAException.XA_RBOTHER);
                    }
                } finally {
                    // commit the transaction
                    try {
                        TransactionUtil.commit(beganTx);
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, module);
                    }
                }
            } catch (GenericTransactionException e) {
                Debug.logError(e, module);
            } finally {
                // resume the transaction
                if (parentTx != null) {
                    try {
                        TransactionUtil.resume(parentTx);
                    } catch (Exception e) {
                        Debug.logError(e, module);
                    }
                }
            }
        } else {
            if (Debug.verboseOn()) Debug.logVerbose("No " + msgPrefix + "service defined; nothing to do", module);
        }

        this.xid = null;
        this.active = false;
    }
}
