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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.config.ServiceConfigListener;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.config.model.ServiceConfig;
import org.apache.ofbiz.service.config.model.ThreadPool;

/**
 * Job poller. Queues and runs jobs.
 */
public final class JobPoller implements ServiceConfigListener {

    private static final String MODULE = JobPoller.class.getName();
    private static final AtomicInteger CREATED = new AtomicInteger();
    private static final ConcurrentHashMap<String, JobManager> JOB_MANAGERS = new ConcurrentHashMap<>();
    private static final ThreadPoolExecutor EXECUTOR = createThreadPoolExecutor();
    private static final JobPoller INSTANCE = new JobPoller();

    /**
     * Returns the <code>JobPoller</code> instance.
     */
    public static JobPoller getInstance() {
        return INSTANCE;
    }

    private static ThreadPoolExecutor createThreadPoolExecutor() {
        try {
            ThreadPool threadPool = ServiceConfigUtil.getServiceEngine(ServiceConfigUtil.getEngine()).getThreadPool();
            return new ThreadPoolExecutor(
                    threadPool.getMinThreads(),
                    threadPool.getMaxThreads(),
                    threadPool.getTtl(),
                    TimeUnit.MILLISECONDS,
                    new PriorityBlockingQueue<>(threadPool.getJobs(), createPriorityComparator()),
                    new JobInvokerThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());
        } catch (GenericConfigException e) {
            Debug.logError(e, "Exception thrown while getting <thread-pool> model, using default <thread-pool> values: ", MODULE);
            return new ThreadPoolExecutor(
                    ThreadPool.MIN_THREADS,
                    ThreadPool.MAX_THREADS,
                    ThreadPool.THREAD_TTL,
                    TimeUnit.MILLISECONDS,
                    new PriorityBlockingQueue<>(ThreadPool.QUEUE_SIZE, createPriorityComparator()),
                    new JobInvokerThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());
        }
    }

    private static Comparator<Runnable> createPriorityComparator() {
        return new Comparator<Runnable>() {

            /**
             * Sorts jobs by priority then by start time
             */
            @Override
            public int compare(Runnable o1, Runnable o2) {
                Job j1 = (Job) o1;
                Job j2 = (Job) o2;
                // Descending priority (higher number returns -1)
                int priorityCompare = Long.compare(j2.getPriority(), j1.getPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                // Ascending start time (earlier time returns -1)
                return Long.compare(j1.getStartTime().getTime(), j2.getStartTime().getTime());
            }
        };
    }

    private static int pollWaitTime() {
        try {
            ThreadPool threadPool = ServiceConfigUtil.getServiceEngine(ServiceConfigUtil.getEngine()).getThreadPool();
            return threadPool.getPollDbMillis();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Exception thrown while getting <thread-pool> model, using default <thread-pool> values: ", MODULE);
            return ThreadPool.POLL_WAIT;
        }
    }

    static int queueSize() {
        try {
            ThreadPool threadPool = ServiceConfigUtil.getServiceEngine(ServiceConfigUtil.getEngine()).getThreadPool();
            return threadPool.getJobs();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Exception thrown while getting <thread-pool> model, using default <thread-pool> values: ", MODULE);
            return ThreadPool.QUEUE_SIZE;
        }
    }

    /**
     * Register a {@link JobManager} with the job poller.
     * @param jm The <code>JobManager</code> to register.
     * @throws IllegalArgumentException if <code>jm</code> is null
     */
    public static void registerJobManager(JobManager jm) {
        Assert.notNull("jm", jm);
        JOB_MANAGERS.putIfAbsent(jm.getDelegator().getDelegatorName(), jm);
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
        Map<String, Object> poolState = new HashMap<>();
        poolState.put("keepAliveTimeInSeconds", EXECUTOR.getKeepAliveTime(TimeUnit.SECONDS));
        poolState.put("numberOfCoreInvokerThreads", EXECUTOR.getCorePoolSize());
        poolState.put("currentNumberOfInvokerThreads", EXECUTOR.getPoolSize());
        poolState.put("numberOfActiveInvokerThreads", EXECUTOR.getActiveCount());
        poolState.put("maxNumberOfInvokerThreads", EXECUTOR.getMaximumPoolSize());
        poolState.put("greatestNumberOfInvokerThreads", EXECUTOR.getLargestPoolSize());
        poolState.put("numberOfCompletedTasks", EXECUTOR.getCompletedTaskCount());
        BlockingQueue<Runnable> queue = EXECUTOR.getQueue();
        List<Map<String, Object>> taskList = new ArrayList<>();
        Map<String, Object> taskInfo = null;
        for (Runnable task : queue) {
            Job job = (Job) task;
            taskInfo = new HashMap<>();
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
        if (!EXECUTOR.isShutdown()) {
            ThreadPool threadPool = serviceConfig.getServiceEngine(ServiceConfigUtil.getEngine()).getThreadPool();
            EXECUTOR.setCorePoolSize(threadPool.getMinThreads());
            EXECUTOR.setMaximumPoolSize(threadPool.getMaxThreads());
            EXECUTOR.setKeepAliveTime(threadPool.getTtl(), TimeUnit.MILLISECONDS);
        }
    }

    private static boolean pollEnabled() {
        try {
            return ServiceConfigUtil.getServiceEngine().getThreadPool().getPollEnabled();
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting configuration: ", MODULE);
            return false;
        }
    }

    /**
     * Adds a job to the job queue.
     * @throws InvalidJobException if the job is in an invalid state.
     */
    public void queueNow(Job job) throws InvalidJobException {
        job.queue();
        try {
            EXECUTOR.execute(job);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            job.deQueue();
        }
    }

    /**
     * Stops the <code>JobPoller</code>. This method is called when OFBiz shuts down.
     * The <code>JobPoller</code> cannot be restarted.
     */
    public void stop() {
        Debug.logInfo("Shutting down JobPoller.", MODULE);
        if (jobManagerPollerThread != null) {
            jobManagerPollerThread.interrupt();
        }
        List<Runnable> queuedJobs = EXECUTOR.shutdownNow();
        for (Runnable task : queuedJobs) {
            try {
                Job queuedJob = (Job) task;
                queuedJob.deQueue();
            } catch (Exception e) {
                Debug.logWarning(e, MODULE);
            }
        }
        Debug.logInfo("JobPoller shutdown completed.", MODULE);
    }

    private static class JobInvokerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "OFBiz-JobQueue-" + CREATED.getAndIncrement());
        }
    }

    // Polls all registered JobManagers for jobs to queue.
    private class JobManagerPoller implements Runnable {

        // Do not check for interrupts in this method. The design requires the
        // thread to complete the job manager poll uninterrupted.
        @Override
        public void run() {
            Debug.logInfo("JobPoller thread started.", MODULE);
            try {
                while (Start.getInstance().getCurrentState() != Start.ServerState.RUNNING) {
                    Thread.sleep(1000);
                }
                while (!EXECUTOR.isShutdown()) {
                    int remainingCapacity = queueSize() - EXECUTOR.getQueue().size();
                    if (remainingCapacity > 0) {
                        // Build "list of lists"
                        Collection<JobManager> jmCollection = JOB_MANAGERS.values();
                        List<Iterator<Job>> pollResults = new ArrayList<>();
                        for (JobManager jm : jmCollection) {
                            if (!jm.isAvailable()) {
                                if (Debug.infoOn()) {
                                    Debug.logInfo("The job manager is locked.", MODULE);
                                }
                                continue;
                            }
                            jm.reloadCrashedJobs();
                            pollResults.add(jm.poll(remainingCapacity).iterator());
                        }
                        // Create queue candidate list from "list of lists"
                        List<Job> queueCandidates = new ArrayList<>();
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
                                Debug.logError(e, MODULE);
                            }
                        }
                    }
                    Thread.sleep(pollWaitTime());
                }
            } catch (InterruptedException e) {
                // Happens when JobPoller shuts down - nothing to do.
                Thread.currentThread().interrupt();
            }
            Debug.logInfo("JobPoller thread stopped.", MODULE);
        }
    }
}
