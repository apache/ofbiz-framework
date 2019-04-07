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
package org.apache.ofbiz.service.job;

import java.util.Date;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;

/**
 * Abstract Job.
 */
public abstract class AbstractJob implements Job {

    public static final String module = AbstractJob.class.getName();

    private final String jobId;
    private final String jobName;
    protected State currentState = State.CREATED;
    private long elapsedTime = 0;
    private final Date startTime = new Date();

    protected AbstractJob(String jobId, String jobName) {
        Assert.notNull("jobId", jobId, "jobName", jobName);
        this.jobId = jobId;
        this.jobName = jobName;
    }

    @Override
    public State currentState() {
        return currentState;
    }

    @Override
    public String getJobId() {
        return this.jobId;
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public void queue() throws InvalidJobException {
        if (currentState != State.CREATED) {
            throw new InvalidJobException("Illegal state change");
        }
        this.currentState = State.QUEUED;
    }

    @Override
    public void deQueue() throws InvalidJobException {
        if (currentState != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
        this.currentState = State.CREATED;
    }

    /**
     *  Executes this Job. The {@link #run()} method calls this method.
     */
    public abstract void exec() throws InvalidJobException;

    @Override
    public void run() {
        long startMillis = System.currentTimeMillis();
        try {
            exec();
        } catch (InvalidJobException e) {
            Debug.logWarning(e.getMessage(), module);
        }
        // sanity check; make sure we don't have any transactions in place
        try {
            // roll back current TX first
            if (TransactionUtil.isTransactionInPlace()) {
                Debug.logWarning("*** NOTICE: JobInvoker finished w/ a transaction in place! Rolling back.", module);
                TransactionUtil.rollback();
            }
            // now resume/rollback any suspended txs
            if (TransactionUtil.suspendedTransactionsHeld()) {
                int suspended = TransactionUtil.cleanSuspendedTransactions();
                Debug.logWarning("Resumed/Rolled Back [" + suspended + "] transactions.", module);
            }
        } catch (GenericTransactionException e) {
            Debug.logWarning(e, module);
        }
        elapsedTime = System.currentTimeMillis() - startMillis;
    }

    @Override
    public long getRuntime() {
        return elapsedTime;
    }

    @Override
    public Date getStartTime() {
        return (Date) startTime.clone();
    }

    /* 
     * Returns JobPriority.NORMAL, the default setting
     */
    @Override
    public long getPriority() {
        return JobPriority.NORMAL;
    }
}
