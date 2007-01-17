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

import java.util.*;

import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.util.Debug;

/**
 * JobPoller - Polls for persisted jobs to run.
 */
public class JobPoller implements Runnable {

    public static final String module = JobPoller.class.getName();

    public static final int MIN_THREADS = 1;
    public static final int MAX_THREADS = 15;
    public static final int MAX_JOBS = 3;
    public static final int POLL_WAIT = 20000;
    //public static final long MAX_TTL = 18000000;

    protected Thread thread = null;
    protected LinkedList pool = null;
    protected LinkedList run = null;
    protected JobManager jm = null;

    protected volatile boolean isRunning = false;

    /**
     * Creates a new JobScheduler
     * @param jm JobManager associated with this scheduler
     */
    public JobPoller(JobManager jm) {
        this.jm = jm;
        this.run = new LinkedList();

        // create the thread pool
        this.pool = createThreadPool();

        // re-load crashed jobs
        this.jm.reloadCrashedJobs();

        // start the thread only if polling is enabled
        if (pollEnabled()) {

            // create the poller thread
            thread = new Thread(this, this.toString());
            thread.setDaemon(false);

            // start the poller
            this.isRunning = true;
            thread.start();
        }
    }

    protected JobPoller() {}

    public synchronized void run() {
        if (Debug.infoOn()) Debug.logInfo("JobPoller: (" + thread.getName() + ") Thread Running...", module);
        try {
            // wait 30 seconds before the first poll
            wait(30000);
        } catch (InterruptedException e) {
        }
        while (isRunning) {
            try {
                // grab a list of jobs to run.
                Iterator poll = jm.poll();

                while (poll.hasNext()) {
                    Job job = (Job) poll.next();

                    if (job.isValid())
                        queueNow(job);
                }
                wait(pollWaitTime());
            } catch (InterruptedException e) {
                Debug.logError(e, module);
                stop();
            }
        }
        if (Debug.infoOn()) Debug.logInfo("JobPoller: (" + thread.getName() + ") Thread ending...", module);
    }

    /**
     * Returns the JobManager
     */
    public JobManager getManager() {
        return jm;
    }

    /**
     * Stops the JobPoller
     */
    public void stop() {
        isRunning = false;
        destroyThreadPool();
    }

    public List getPoolState() {
        List stateList = new ArrayList();
        Iterator i = this.pool.iterator();
        while (i.hasNext()) {
            JobInvoker invoker = (JobInvoker) i.next();
            Map stateMap = new HashMap();
            stateMap.put("threadName", invoker.getName());
            stateMap.put("jobName", invoker.getJobName());
            stateMap.put("serviceName", invoker.getServiceName());
            stateMap.put("runTime", new Long(invoker.getCurrentRuntime()));
            stateMap.put("status", new Integer(invoker.getCurrentStatus()));
            stateList.add(stateMap);
        }
        return stateList;
    }

    /**
     * Stops all threads in the threadPool and clears
     * the pool as final step.
     */
    private void destroyThreadPool() {
        Debug.logInfo("Destroying thread pool...", module);
        Iterator it = pool.iterator();
        while (it.hasNext()) {
            JobInvoker ji = (JobInvoker) it.next();
            ji.stop();
        }
        pool.clear();
    }

    public synchronized void killThread(String threadName) {
        JobInvoker inv = findThread(threadName);
        if (inv != null) {
            inv.kill();
            this.pool.remove(inv);
        }
    }

    private JobInvoker findThread(String threadName) {
        Iterator i = this.pool.iterator();
        while (i.hasNext()) {
            JobInvoker inv = (JobInvoker) i.next();
            if (threadName.equals(inv.getName())) {
                return inv;
            }
        }
        return null;
    }

    /**
     * Returns the next job to run
     */
    public synchronized Job next() {
        if (run.size() > 0)
            return (Job) run.removeFirst();
        return null;
    }

    /**
     * Adds a job to the RUN queue
     */
    public synchronized void queueNow(Job job) {
        run.add(job);
        if (Debug.verboseOn()) Debug.logVerbose("New run queue size: " + run.size(), module);
        if (run.size() > pool.size() && pool.size() < maxThreads()) {
            int calcSize = (run.size() / jobsPerThread()) - (pool.size());
            int addSize = calcSize > maxThreads() ? maxThreads() : calcSize;

            for (int i = 0; i < addSize; i++) {
                JobInvoker iv = new JobInvoker(this, invokerWaitTime());
                pool.add(iv);
            }
        }
    }

    /**
     * Removes a thread from the pool.
     * @param invoker The invoker to remove.
     */
    public synchronized void removeThread(JobInvoker invoker) {
        pool.remove(invoker);
        invoker.stop();
        if (pool.size() < minThreads()) {
            for (int i = 0; i < minThreads() - pool.size(); i++) {
                JobInvoker iv = new JobInvoker(this, invokerWaitTime());
                pool.add(iv);
            }
        }
    }

    // Creates the invoker pool
    private LinkedList createThreadPool() {
        LinkedList threadPool = new LinkedList();

        while (threadPool.size() < minThreads()) {
            JobInvoker iv = new JobInvoker(this, invokerWaitTime());
            threadPool.add(iv);
        }

        return threadPool;
    }

    private int maxThreads() {
        int max = MAX_THREADS;

        try {
            max = Integer.parseInt(ServiceConfigUtil.getElementAttr("thread-pool", "max-threads"));
        } catch (NumberFormatException nfe) {
            Debug.logError("Problems reading values from serviceengine.xml file [" + nfe.toString() + "]. Using defaults.", module);
        }
        return max;
    }

    private int minThreads() {
        int min = MIN_THREADS;

        try {
            min = Integer.parseInt(ServiceConfigUtil.getElementAttr("thread-pool", "min-threads"));
        } catch (NumberFormatException nfe) {
            Debug.logError("Problems reading values from serviceengine.xml file [" + nfe.toString() + "]. Using defaults.", module);
        }
        return min;
    }

    private int jobsPerThread() {
        int jobs = MAX_JOBS;

        try {
            jobs = Integer.parseInt(ServiceConfigUtil.getElementAttr("thread-pool", "jobs"));
        } catch (NumberFormatException nfe) {
            Debug.logError("Problems reading values from serviceengine.xml file [" + nfe.toString() + "]. Using defaults.", module);
        }
        return jobs;
    }

    private int invokerWaitTime() {
        int wait = JobInvoker.WAIT_TIME;

        try {
            wait = Integer.parseInt(ServiceConfigUtil.getElementAttr("thread-pool", "wait-millis"));
        } catch (NumberFormatException nfe) {
            Debug.logError("Problems reading values from serviceengine.xml file [" + nfe.toString() + "]. Using defaults.", module);
        }
        return wait;
    }

    private int pollWaitTime() {
        int poll = POLL_WAIT;

        try {
            poll = Integer.parseInt(ServiceConfigUtil.getElementAttr("thread-pool", "poll-db-millis"));
        } catch (NumberFormatException nfe) {
            Debug.logError("Problems reading values from serviceengine.xml file [" + nfe.toString() + "]. Using defaults.", module);
        }
        return poll;
    }

    private boolean pollEnabled() {
        String enabled = ServiceConfigUtil.getElementAttr("thread-pool", "poll-enabled");

        if (enabled.equalsIgnoreCase("false"))
            return false;

        // also make sure we have a delegator to use for polling
        if (jm.getDelegator() == null) {
            Debug.logWarning("No delegator referenced; not starting job poller.", module);
            return false;
        }

        return true;
    }
}

