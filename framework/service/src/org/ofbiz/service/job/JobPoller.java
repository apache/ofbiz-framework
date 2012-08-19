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

import org.ofbiz.base.start.Start;
import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.Debug;
import org.ofbiz.service.config.ServiceConfigUtil;

/**
 * Job poller. Queues and runs jobs.
 */
public final class JobPoller {

    public static final String module = JobPoller.class.getName();
    private static final AtomicInteger created = new AtomicInteger();
    private static final int MIN_THREADS = 1; // Must be no less than one or the executor will shut down.
    private static final int MAX_THREADS = 5; // Values higher than 5 might slow things down.
    private static final int POLL_WAIT = 30000; // Database polling interval - 30 seconds.
    private static final int QUEUE_SIZE = 100;
    private static final long THREAD_TTL = 120000; // Idle thread lifespan - 2 minutes.
    private static final ConcurrentHashMap<String, JobManager> jobManagers = new ConcurrentHashMap<String, JobManager>();
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(minThreads(), maxThreads(), getTTL(),
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(queueSize()), new JobInvokerThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    private static final JobPoller instance = new JobPoller();

    /**
     * Returns the <code>JobPoller</code> instance.
     */
    public static JobPoller getInstance() {
        return instance;
    }

    private static long getTTL() {
        String threadTTLAttr = ServiceConfigUtil.getElementAttr("thread-pool", "ttl");
        if (!threadTTLAttr.isEmpty()) {
            try {
                int threadTTL = Integer.parseInt(threadTTLAttr);
                if (threadTTL > 0) {
                    return threadTTL;
                }
            } catch (NumberFormatException e) {
                Debug.logError("Exception thrown while parsing thread TTL from serviceengine.xml file [" + e + "]. Using default value.", module);
            }
        }
        return THREAD_TTL;
    }

    private static int maxThreads() {
        String maxThreadsAttr = ServiceConfigUtil.getElementAttr("thread-pool", "max-threads");
        if (!maxThreadsAttr.isEmpty()) {
            try {
                int maxThreads = Integer.parseInt(maxThreadsAttr);
                if (maxThreads > 0) {
                    return maxThreads;
                }
            } catch (NumberFormatException e) {
                Debug.logError("Exception thrown while parsing maximum threads from serviceengine.xml file [" + e + "]. Using default value.", module);
            }
        }
        return MAX_THREADS;
    }

    private static int minThreads() {
        String minThreadsAttr = ServiceConfigUtil.getElementAttr("thread-pool", "min-threads");
        if (!minThreadsAttr.isEmpty()) {
            try {
                int minThreads = Integer.parseInt(minThreadsAttr);
                if (minThreads > 0) {
                    return minThreads;
                }
            } catch (NumberFormatException e) {
                Debug.logError("Exception thrown while parsing minimum threads from serviceengine.xml file [" + e + "]. Using default value.", module);
            }
        }
        return MIN_THREADS;
    }

    private static int pollWaitTime() {
        String pollIntervalAttr = ServiceConfigUtil.getElementAttr("thread-pool", "poll-db-millis");
        if (!pollIntervalAttr.isEmpty()) {
            try {
                int pollInterval = Integer.parseInt(pollIntervalAttr);
                if (pollInterval > 0) {
                    return pollInterval;
                }
            } catch (NumberFormatException e) {
                Debug.logError("Exception thrown while parsing database polling interval from serviceengine.xml file [" + e + "]. Using default value.", module);
            }
        }
        return POLL_WAIT;
    }

    private static int queueSize() {
        String queueSizeAttr = ServiceConfigUtil.getElementAttr("thread-pool", "jobs");
        if (!queueSizeAttr.isEmpty()) {
            try {
                int queueSize = Integer.parseInt(queueSizeAttr);
                if (queueSize > 0) {
                    return queueSize;
                }
            } catch (NumberFormatException e) {
                Debug.logError("Exception thrown while parsing queue size from serviceengine.xml file [" + e + "]. Using default value.", module);
            }
        }
        return QUEUE_SIZE;
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

    private boolean pollEnabled() {
        String enabled = ServiceConfigUtil.getElementAttr("thread-pool", "poll-enabled");
        return !"false".equalsIgnoreCase(enabled);
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
