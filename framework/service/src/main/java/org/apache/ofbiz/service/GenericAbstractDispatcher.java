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

import java.util.Date;
import java.util.Map;

import javax.transaction.Transaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.calendar.RecurrenceRule;
import org.apache.ofbiz.service.jms.JmsListenerFactory;
import org.apache.ofbiz.service.job.JobManager;
import org.apache.ofbiz.service.job.JobManagerException;

/**
 * Generic Services Local Dispatcher
 */
public abstract class GenericAbstractDispatcher implements LocalDispatcher {

    public static final String module = GenericAbstractDispatcher.class.getName();

    protected DispatchContext ctx = null;
    protected ServiceDispatcher dispatcher = null;
    protected String name = null;

    public GenericAbstractDispatcher() {}

    @Override
    public void schedule(String poolName, String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count, long endTime, int maxRetry) throws GenericServiceException {
        schedule(null, poolName, serviceName, context, startTime, frequency, interval, count, endTime, maxRetry);
    }

    @Override
    public void schedule(String poolName, String serviceName, long startTime, int frequency, int interval, int count, long endTime, int maxRetry, Object... context) throws GenericServiceException {
        schedule(poolName, serviceName, ServiceUtil.makeContext(context), startTime, frequency, interval, count, endTime, maxRetry);
    }

    @Override
    public void schedule(String jobName, String poolName, String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count, long endTime, int maxRetry) throws GenericServiceException {
        Transaction suspendedTransaction = null;
        try {
            boolean beganTransaction = false;
            suspendedTransaction = TransactionUtil.suspend();
            try {
                beganTransaction = TransactionUtil.begin();
                try {
                    getJobManager().schedule(jobName, poolName, serviceName, context, startTime, frequency, interval, count, endTime, maxRetry);

                    if (Debug.verboseOn()) {
                        Debug.logVerbose("[LocalDispatcher.schedule] : Current time : " + (new Date()).getTime(), module);
                        Debug.logVerbose("[LocalDispatcher.schedule] : Runtime      : " + startTime, module);
                        Debug.logVerbose("[LocalDispatcher.schedule] : Frequency    : " + frequency, module);
                        Debug.logVerbose("[LocalDispatcher.schedule] : Interval     : " + interval, module);
                        Debug.logVerbose("[LocalDispatcher.schedule] : Count        : " + count, module);
                        Debug.logVerbose("[LocalDispatcher.schedule] : EndTime      : " + endTime, module);
                        Debug.logVerbose("[LocalDispatcher.schedule] : MazRetry     : " + maxRetry, module);
                    }

                } catch (JobManagerException jme) {
                    throw new GenericServiceException(jme.getMessage(), jme);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String errMsg = "General error while scheduling job";
                Debug.logError(e, errMsg, module);
                try {
                    TransactionUtil.rollback(beganTransaction, errMsg, e);
                } catch (GenericTransactionException gte1) {
                    Debug.logError(gte1, "Unable to rollback transaction", module);
                }
            } finally {
                try {
                    TransactionUtil.commit(beganTransaction);
                } catch (GenericTransactionException gte2) {
                    Debug.logError(gte2, "Unable to commit scheduled job", module);
                }
            }
        } catch (GenericTransactionException gte) {
            Debug.logError(gte, "Error suspending transaction while scheduling job", module);
        } finally {
            if (suspendedTransaction != null) {
                try {
                    TransactionUtil.resume(suspendedTransaction);
                } catch (GenericTransactionException gte3) {
                    Debug.logError(gte3, "Error resuming suspended transaction after scheduling job", module);
                }
            }
        }
    }

    @Override
    public void schedule(String jobName, String poolName, String serviceName, long startTime, int frequency, int interval, int count, long endTime, int maxRetry, Object... context) throws GenericServiceException {
        schedule(jobName, poolName, serviceName, ServiceUtil.makeContext(context), startTime, frequency, interval, count, endTime, maxRetry);
    }

    @Override
    public void addRollbackService(String serviceName, Map<String, ? extends Object> context, boolean persist) throws GenericServiceException {
        ServiceSynchronization.registerRollbackService(this.getDispatchContext(), serviceName, null, context, true, persist);
    }

    @Override
    public void addRollbackService(String serviceName, boolean persist, Object... context) throws GenericServiceException {
        addRollbackService(serviceName, ServiceUtil.makeContext(context), persist);
    }

    @Override
    public void addCommitService(String serviceName, Map<String, ? extends Object> context, boolean persist) throws GenericServiceException {
        ServiceSynchronization.registerCommitService(this.getDispatchContext(), serviceName, null, context, true, persist);
    }

    @Override
    public void addCommitService(String serviceName, boolean persist, Object... context) throws GenericServiceException {
        addCommitService(serviceName, ServiceUtil.makeContext(context), persist);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count, long endTime) throws GenericServiceException {
        ModelService model = ctx.getModelService(serviceName);
        schedule(null, serviceName, context, startTime, frequency, interval, count, endTime, model.maxRetry);
    }

    @Override
    public void schedule(String serviceName, long startTime, int frequency, int interval, int count, long endTime, Object... context) throws GenericServiceException {
        schedule(serviceName, ServiceUtil.makeContext(context), startTime, frequency, interval, count, endTime);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count) throws GenericServiceException {
        schedule(serviceName, context, startTime, frequency, interval, count, 0);
    }

    @Override
    public void schedule(String serviceName, long startTime, int frequency, int interval, int count, Object... context) throws GenericServiceException {
        schedule(serviceName, ServiceUtil.makeContext(context), startTime, frequency, interval, count);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, long endTime) throws GenericServiceException {
        schedule(serviceName, context, startTime, frequency, interval, -1, endTime);
    }

    @Override
    public void schedule(String serviceName, long startTime, int frequency, int interval, long endTime, Object... context) throws GenericServiceException {
        schedule(serviceName, ServiceUtil.makeContext(context), startTime, frequency, interval, endTime);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime) throws GenericServiceException {
        schedule(serviceName, context, startTime, RecurrenceRule.DAILY, 1, 1);
    }

    @Override
    public void schedule(String serviceName, long startTime, Object... context) throws GenericServiceException {
        schedule(serviceName, ServiceUtil.makeContext(context), startTime);
    }

    @Override
    public JobManager getJobManager() {
        return dispatcher.getJobManager();
    }

    @Override
    public JmsListenerFactory getJMSListeneFactory() {
        return dispatcher.getJMSListenerFactory();
    }

    @Override
    public Delegator getDelegator() {
        return dispatcher.getDelegator();
    }

    @Override
    public Security getSecurity() {
        return dispatcher.getSecurity();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public DispatchContext getDispatchContext() {
        return ctx;
    }

    @Override
    public void deregister() {
        ServiceContainer.removeFromCache(getName());
        dispatcher.deregister(this);
    }

    @Override
    public void registerCallback(String serviceName, GenericServiceCallback cb) {
        dispatcher.registerCallback(serviceName, cb);
    }
}

