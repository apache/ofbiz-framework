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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.config.ServiceConfigListener;
import org.apache.ofbiz.service.config.model.ServiceConfig;
import org.apache.ofbiz.service.config.model.ThreadPool;

/**
 * Job poller. Queues and runs jobs.
 */
public final class JobPoller implements ServiceConfigListener {

    public static final String module = JobPoller.class.getName();
    private static final AtomicInteger created = new AtomicInteger();
    private static final ConcurrentHashMap<String, JobManager> jobManagers = new ConcurrentHashMap<String, JobManager>();
    private static final ThreadPoolExecutor executor = createThreadPoolExecutor();
    private static final JobPoller instance = new JobPoller();

    /**
     * Returns the <code>JobPoller</code> instance.
     */
    public static JobPoller getInstance() {
        return instance;
    }

    private static ThreadPoolExecutor createThreadPoolExecutor() {
        try {
            ThreadPool threadPool = ServiceConfigUtil.getServiceEngine(ServiceConfigUtil.getEngine()).getThreadPool();
            return new ThreadPoolExecutor(threadPool.getMinThreads(), threadPool.getMaxThreads(), threadPool.getTtl(),
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(threadPool.getJobs()), new JobInvokerThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        } catch (GenericConfigException e) {
            Debug.logError(e, "Exception thrown while getting <thread-pool> model, using default <thread-pool> values: ", module);
            return new ThreadPoolExecutor(ThreadPool.MIN_THREADS, ThreadPool.MAX_THREADS, ThreadPool.THREAD_TTL,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(ThreadPool.QUEUE_SIZE), new JobInvokerThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        }
    }

    private static int pollWaitTime() {
        try {
            ThreadPool threadPool = ServiceConfigUtil.getServiceEngine(ServiceConfigUtil.getEngine()).getThreadPool();
            return threadPool.getPollDbMillis();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Exception thrown while getting <thread-pool> model, using default <thread-pool> values: ", module);
            return ThreadPool.POLL_WAIT;
        }
    }

    /**
     * Register a {@link JobManager} with the job poller.
     * 
     * @param jm The <code>JobManager</code> to register.
     * @throws IllegalArgumentException if <code>jm</code> is null
     */
    public static void registerJobManager(JobManager jm) {
        Assert.notNull("jm", jm);
        jobManagers.putIfAbsent(jm.getDelegator().getDelegatorName(), jm);
    }

    // -------------------------------------- //

    private final Thread jobManagerPollerThread;

    private JobPoller() {
        if (pollEnabled()) {
            jobManagerPollerThread = new Thread(new JobManagerPoller(), "OFBiz-JobPoller");
            jobManagerPollerThread.setDaemon(false);
            jobManagerPollerThread.start();
        } else {
            jobManagerPollerThread = null;
        }
        ServiceConfigUtil.registerServiceConfigListener(this);
    }

    /**
     * Returns a <code>Map</code> containing <code>JobPoller</code> statistics.
     */
    public Map<String, Object> getPoolState() {
        Map<String, Object> poolState = new HashMap<String, Object>();
        poolState.put("keepAliveTimeInSeconds", executor.getKeepAliveTime(TimeUnit.SECONDS));
        poolState.put("numberOfCoreInvokerThreads", executor.getCorePoolSize());
        poolState.put("currentNumberOfInvokerThreads", executor.getPoolSize());
        poolState.put("numberOfActiveInvokerThreads", executor.getActiveCount());
        poolState.put("maxNumberOfInvokerThreads", executor.getMaximumPoolSize());
        poolState.put("greatestNumberOfInvokerThreads", executor.getLargestPoolSize());
        poolState.put("numberOfCompletedTasks", executor.getCompletedTaskCount());
        BlockingQueue<Runnable> queue = executor.getQueue();
        List<Map<String, Object>> taskList = new ArrayList<Map<String, Object>>();
        Map<String, Object> taskInfo = null;
        for (Runnable task : queue) {
            Job job = (Job) task;
            taskInfo = new HashMap<String, Object>();
            taskInfo.put("id", job.getJobId());
            taskInfo.put("name", job.getJobName());
            String serviceName = "";
            if (job instanceof GenericServiceJob) {
                serviceName = ((GenericServiceJob) job).getServiceName();
            }
            taskInfo.put("serviceName", serviceName);
            taskInfo.put("time", job.getStartTime());
            taskInfo.put("runtime", job.getRuntime());
            taskList.add(taskInfo);
        }
        poolState.put("taskList", taskList);
        return poolState;
    }

    @Override
    public void onServiceConfigChange(ServiceConfig serviceConfig) {
        if (!executor.isShutdown()) {
            ThreadPool threadPool = serviceConfig.getServiceEngine(ServiceConfigUtil.getEngine()).getThreadPool();
            executor.setCorePoolSize(threadPool.getMinThreads());
            executor.setMaximumPoolSize(threadPool.getMaxThreads());
            executor.setKeepAliveTime(threadPool.getTtl(), TimeUnit.MILLISECONDS);
        }
    }

    private boolean pollEnabled() {
        try {
            return ServiceConfigUtil.getServiceEngine().getThreadPool().getPollEnabled();
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting configuration: ", module);
            return false;
        }
    }

    /**
     * Adds a job to the job queue.
     * @throws InvalidJobException if the job is in an invalid state.
     * @throws RejectedExecutionException if the poller is stopped.
     */
    public void queueNow(Job job) throws InvalidJobException {
        job.queue();
        try {
            executor.execute(job);
        } catch (Exception e) {
            job.deQueue();
        }
    }

    /**
     * Stops the <code>JobPoller</code>. This method is called when OFBiz shuts down.
     * The <code>JobPoller</code> cannot be restarted.
     */
    public void stop() {
        Debug.logInfo("Shutting down JobPoller.", module);
        if (jobManagerPollerThread != null) {
            jobManagerPollerThread.interrupt();
        }
        List<Runnable> queuedJobs = executor.shutdownNow();
        for (Runnable task : queuedJobs) {
            try {
                Job queuedJob = (Job) task;
                queuedJob.deQueue();
            } catch (Exception e) {
                Debug.logWarning(e, module);
            }
        }
        Debug.logInfo("JobPoller shutdown completed.", module);
    }

    private static class JobInvokerThreadFactory implements ThreadFactory {

        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "OFBiz-JobQueue-" + created.getAndIncrement());
        }
    }

    // Polls all registered JobManagers for jobs to queue.
    private class JobManagerPoller implements Runnable {

        // Do not check for interrupts in this method. The design requires the
        // thread to complete the job manager poll uninterrupted.
        public void run() {
            Debug.logInfo("JobPoller thread started.", module);
            try {
                while (Start.getInstance().getCurrentState() != Start.ServerState.RUNNING) {
                    Thread.sleep(1000);
                }
                while (!executor.isShutdown()) {
                    int remainingCapacity = executor.getQueue().remainingCapacity();
                    if (remainingCapacity > 0) {
                        // Build "list of lists"
                        Collection<JobManager> jmCollection = jobManagers.values();
                        List<Iterator<Job>> pollResults = new ArrayList<Iterator<Job>>();
                        for (JobManager jm : jmCollection) {
                            if (!jm.isAvailable()) {
                                if (Debug.infoOn()) Debug.logInfo("The job manager is locked.", module);
                                continue;
                            }
                            jm.reloadCrashedJobs();
                            pollResults.add(jm.poll(remainingCapacity).iterator());
                        }
                        // Create queue candidate list from "list of lists"
                        List<Job> queueCandidates = new ArrayList<Job>();
                        boolean addingJobs = true;
                        while (addingJobs) {
                            addingJobs = false;
                            for (Iterator<Job> jobIterator : pollResults) {
                                if (jobIterator.hasNext()) {
                                    queueCandidates.add(jobIterator.next());
                                    addingJobs = true;
                                }
                            }
                        }
                        // The candidate list might be larger than the queue remaining capacity,
                        // but that is okay - the excess jobs will be dequeued and rescheduled.
                        for (Job job : queueCandidates) {
                            try {
                                queueNow(job);
                            } catch (InvalidJobException e) {
                                Debug.logError(e, module);
                            }
                        }
                    }
                    Thread.sleep(pollWaitTime());
                }
            } catch (InterruptedException e) {
                // Happens when JobPoller shuts down - nothing to do.
                Thread.currentThread().interrupt();
            }
            Debug.logInfo("JobPoller thread stopped.", module);
        }
    }
}
