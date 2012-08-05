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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.config.ServiceConfigUtil;

import org.apache.commons.lang.math.NumberUtils;

/**
 * Job poller. Queues and runs jobs.
 */
public final class JobPoller implements Runnable {

    public static final String module = JobPoller.class.getName();
    public static final int MIN_THREADS = 1;
    public static final int MAX_THREADS = 15;
    public static final int POLL_WAIT = 20000;
    public static final long THREAD_TTL = 18000000;

    private final JobManager jm;
    private final ThreadPoolExecutor executor;
    private final String name;
    private boolean enabled = false;

    /**
     * Creates a new JobScheduler
     * 
     * @param jm
     *            JobManager associated with this scheduler
     */
    public JobPoller(JobManager jm) {
        this.name = jm.getDelegator().getDelegatorName();
        this.jm = jm;
        this.executor = new ThreadPoolExecutor(minThreads(), maxThreads(), getTTL(), TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                new JobInvokerThreadFactory(this.name), new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized void enable() {
        if (!enabled) {
            enabled = true;
            // start the thread only if polling is enabled
            if (pollEnabled()) {
                // create the poller thread
                Thread thread = new Thread(this, "OFBiz-JobPoller-" + this.name);
                thread.setDaemon(false);
                // start the poller
                thread.start();
            }
        }
    }

    /**
     * Returns the JobManager
     */
    public JobManager getManager() {
        return jm;
    }

    public Map<String, Object> getPoolState() {
        Map<String, Object> poolState = new HashMap<String, Object>();
        poolState.put("pollerName", this.name);
        poolState.put("pollerThreadName", "OFBiz-JobPoller-" + this.name);
        poolState.put("invokerThreadNameFormat", "OFBiz-JobInvoker-" + this.name + "-<SEQ>");
        poolState.put("keepAliveTimeInSeconds", this.executor.getKeepAliveTime(TimeUnit.SECONDS));
        poolState.put("numberOfCoreInvokerThreads", this.executor.getCorePoolSize());
        poolState.put("currentNumberOfInvokerThreads", this.executor.getPoolSize());
        poolState.put("numberOfActiveInvokerThreads", this.executor.getActiveCount());
        poolState.put("maxNumberOfInvokerThreads", this.executor.getMaximumPoolSize());
        poolState.put("greatestNumberOfInvokerThreads", this.executor.getLargestPoolSize());
        poolState.put("numberOfCompletedTasks", this.executor.getCompletedTaskCount());
        BlockingQueue<Runnable> queue = this.executor.getQueue();
        List<Map<String, Object>> taskList = new ArrayList<Map<String, Object>>();
        Map<String, Object> taskInfo = null;
        for (Runnable task : queue) {
            JobInvoker jobInvoker = (JobInvoker) task;
            taskInfo = new HashMap<String, Object>();
            taskInfo.put("id", jobInvoker.getJobId());
            taskInfo.put("name", jobInvoker.getJobName());
            taskInfo.put("serviceName", jobInvoker.getServiceName());
            taskInfo.put("time", jobInvoker.getTime());
            taskInfo.put("runtime", jobInvoker.getCurrentRuntime());
            taskList.add(taskInfo);
        }
        poolState.put("taskList", taskList);
        return poolState;
    }

    private long getTTL() {
        long ttl = THREAD_TTL;
        try {
            ttl = NumberUtils.toLong(ServiceConfigUtil.getElementAttr("thread-pool", "ttl"));
        } catch (NumberFormatException nfe) {
            Debug.logError("Problems reading value from attribute [ttl] of element [thread-pool] in serviceengine.xml file [" + nfe.toString() + "]. Using default (" + THREAD_TTL + ").", module);
        }
        return ttl;
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

    private boolean pollEnabled() {
        String enabled = ServiceConfigUtil.getElementAttr("thread-pool", "poll-enabled");
        if (enabled.equalsIgnoreCase("false"))
            return false;
        return true;
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

    /**
     * Adds a job to the RUN queue.
     * @throws InvalidJobException if the job is in an invalid state.
     * @throws RejectedExecutionException if the poller is stopped.
     */
    public void queueNow(Job job) throws InvalidJobException {
        job.queue();
        this.executor.execute(new JobInvoker(job));
    }

    public synchronized void run() {
        try {
            // wait 30 seconds before the first poll
            java.lang.Thread.sleep(30000);
        } catch (InterruptedException e) {
        }
        while (!executor.isShutdown()) {
            try {
                // grab a list of jobs to run.
                List<Job> pollList = jm.poll();
                for (Job job : pollList) {
                    try {
                        queueNow(job);
                    } catch (InvalidJobException e) {
                        Debug.logError(e, module);
                    }
                }
                // NOTE: using sleep instead of wait for stricter locking
                java.lang.Thread.sleep(pollWaitTime());
            } catch (InterruptedException e) {
                Debug.logError(e, module);
                stop();
            }
        }
        Debug.logInfo("JobPoller " + this.name + " thread terminated.", module);
    }

    /**
     * Stops the JobPoller
     */
    void stop() {
        Debug.logInfo("Shutting down thread pool for JobPoller " + this.name, module);
        this.executor.shutdown();
        try {
            // Wait 60 seconds for existing tasks to terminate
            if (!this.executor.awaitTermination(60, TimeUnit.SECONDS)) {
                // abrupt shutdown (cancel currently executing tasks)
                Debug.logInfo("Attempting abrupt shut down of thread pool for JobPoller " + this.name, module);
                this.executor.shutdownNow();
                // Wait 60 seconds for tasks to respond to being cancelled
                if (!this.executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    Debug.logWarning("Unable to shutdown the thread pool for JobPoller " + this.name, module);
                }
            }
        } catch (InterruptedException ie) {
            // re cancel if current thread was also interrupted
            this.executor.shutdownNow();
            // preserve interrupt status
            Thread.currentThread().interrupt();
        }
        Debug.logInfo("Shutdown completed of thread pool for JobPoller " + this.name, module);
    }
}
