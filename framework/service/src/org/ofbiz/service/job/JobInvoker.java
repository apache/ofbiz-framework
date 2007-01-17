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

import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.transaction.GenericTransactionException;

/**
 * JobInvoker
 */
public class JobInvoker implements Runnable {

    public static final String module = JobInvoker.class.getName();
    public static final long THREAD_TTL = 18000000;
    public static final int WAIT_TIME = 750;

    private JobPoller jp = null;
    private Thread thread = null;
    private Date created = null;
    private String name = null;
    private int count = 0;
    private int wait = 0;

    private volatile boolean run = false;
    private volatile Job currentJob = null;
    private volatile int statusCode = 0;
    private volatile long jobStart = 0;

    public JobInvoker(JobPoller jp) {
        this(jp, WAIT_TIME);
    }

    public JobInvoker(JobPoller jp, int wait) {
        this.created = new Date();
        this.run = true;
        this.count = 0;
        this.jp = jp;
        this.wait = wait;

        // service dispatcher delegator name (for thread name)
        String delegatorName = jp.getManager().getDelegator().getDelegatorName();

        // get a new thread
        this.thread = new Thread(this);
        this.name = delegatorName + "-invoker-" + this.thread.getName();

        this.thread.setDaemon(false);
        this.thread.setName(this.name);

        if (Debug.verboseOn()) Debug.logVerbose("JobInoker: Starting Invoker Thread -- " + thread.getName(), module);
        this.thread.start();
    }

    protected JobInvoker() {}

    /**
     * Tells the thread to stop after the next job.
     */
    public void stop() {
        run = false;
    }

    /**
     * Wakes up this thread.
     */
    public void wakeUp() {
        notifyAll();
    }

    /**
     * Gets the number of times this thread was used.
     * @return The number of times used.
     */
    public int getUsage() {
        return count;
    }

    /**
     * Gets the time when this thread was created.
     * @return Time in milliseconds when this was created.
     */
    public long getTime() {
        return created.getTime();
    }

    /**
     * Gets the name of this JobInvoker.
     * @return Name of the invoker.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the status code for this thread (0 = sleeping, 1 = running job)
     * @return 0 for sleeping or 1 when running a job.
     */
    public int getCurrentStatus() {
        return this.statusCode;
    }

    /**
     * Gets the total time the current job has been running or 0 when sleeping.
     * @return Total time the curent job has been running.
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
        if (this.statusCode == 1) {
            if (this.currentJob != null) {
                return this.currentJob.getJobId();
            } else {
                return "WARNING: Invalid Job!";
            }
        } else {
            return null;
        }
    }

    /**
     * Get the current running job's name.
     * @return String name of the current running job.
     */
    public String getJobName() {
        if (this.statusCode == 1) {
            if (this.currentJob != null) {
                return this.currentJob.getJobName();
            } else {
                return "WARNING: Invalid Job!";
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the service being run.
     * @return The name of the service being run.
     */
    public String getServiceName() {
        String serviceName = null;
        if (this.statusCode == 1) {
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
        }
        return serviceName;
    }

    /**
     * Kill this invoker thread.s
     */
    public void kill() {
        this.stop();
        this.statusCode = -1;
        this.thread.interrupt();
        this.thread = null;
    }

    public synchronized void run() {
        while (run) {
            Job job = jp.next();

            if (job == null) {
                try {
                    wait(wait);
                } catch (InterruptedException ie) {
                    Debug.logError(ie, "JobInvoker.run() : InterruptedException", module);
                    stop();
                }
            } else {
                Debug.log("Invoker: " + thread.getName() + " received job -- " + job.getJobName() + " from poller - " + jp.toString(), module);
                
                // setup the current job settings
                this.currentJob = job;
                this.statusCode = 1;
                this.jobStart = System.currentTimeMillis();

                // execute the job
                if (Debug.verboseOn()) Debug.logVerbose("Invoker: " + thread.getName() + " executing job -- " + job.getJobName(), module);
                try {
                    job.exec();
                } catch (InvalidJobException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
                if (Debug.verboseOn()) Debug.logVerbose("Invoker: " + thread.getName() + " finished executing job -- " + job.getJobName(), module);

                // clear the current job settings
                this.currentJob = null;
                this.statusCode = 0;
                this.jobStart = 0;

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

                // increment the count
                count++;
                if (Debug.verboseOn()) Debug.logVerbose("Invoker: " + thread.getName() + " (" + count + ") total.", module);
            }
            long diff = (new Date().getTime() - this.getTime());

            if (getTTL() > 0 && diff > getTTL())
                jp.removeThread(this);
        }
        if (Debug.verboseOn()) Debug.logVerbose("Invoker: " + thread.getName() + " dead -- " + UtilDateTime.nowTimestamp(), module);
    }

    private long getTTL() {
        long ttl = THREAD_TTL;

        try {
            ttl = Long.parseLong(ServiceConfigUtil.getElementAttr("thread-pool", "ttl"));
        } catch (NumberFormatException nfe) {
            Debug.logError("Problems reading values from serviceengine.xml file [" + nfe.toString() + "]. Using defaults.", module);
        }
        return ttl;
    }
}
