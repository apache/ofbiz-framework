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
package org.ofbiz.service.job;

import java.util.Date;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

/**
 * JobInvoker
 */
public class JobInvoker implements Runnable {

    public static final String module = JobInvoker.class.getName();

    private Date created = null;
    private long jobStart;

    private Job currentJob = null;

    public JobInvoker(Job job) {
        this.created = new Date();
        this.currentJob = job;
    }

    protected JobInvoker() {}

    /**
     * Gets the time when this thread was created.
     * @return Time in milliseconds when this was created.
     */
    public long getTime() {
        return created.getTime();
    }

    /**
     * Gets the total time the current job has been running or 0 when sleeping.
     * @return Total time the current job has been running.
     */
    public long getCurrentRuntime() {
        if (this.jobStart > 0) {
            long now = System.currentTimeMillis();
            return now - this.jobStart;
        } else {
            return 0;
        }
    }

    /**
     * Get the current running job's ID.
     * @return String ID of the current running job.
     */
    public String getJobId() {
        if (this.currentJob != null) {
            return this.currentJob.getJobId();
        } else {
            return "WARNING: Invalid Job!";
        }
    }

    /**
     * Get the current running job's name.
     * @return String name of the current running job.
     */
    public String getJobName() {
        if (this.currentJob != null) {
            return this.currentJob.getJobName();
        } else {
            return "WARNING: Invalid Job!";
        }
    }

    /**
     * Returns the name of the service being run.
     * @return The name of the service being run.
     */
    public String getServiceName() {
        String serviceName = null;
        if (this.currentJob != null) {
            if (this.currentJob instanceof GenericServiceJob) {
                GenericServiceJob gsj = (GenericServiceJob) this.currentJob;
                try {
                    serviceName = gsj.getServiceName();
                } catch (InvalidJobException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return serviceName;
    }

    public void run() {
        // setup the current job settings
        this.jobStart = System.currentTimeMillis();

        // execute the job
        try {
            this.currentJob.exec();
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
    }
}
