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
package org.apache.ofbiz.service.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;thread-pool&gt;</code> element.
 */
@ThreadSafe
public final class ThreadPool {

    private static final String MODULE = ThreadPool.class.getName();

    public static final int FAILED_RETRY_MIN = 30;
    public static final int MIN_THREADS = 1; // Must be no less than one or the executor will shut down.
    public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() + 1;
    // See https://stackoverflow.com/questions/13834692/threads-configuration-based-on-no-of-cpu-cores for more
    public static final int POLL_WAIT = 30000; // Database polling interval - 30 seconds.
    public static final int PURGE_JOBS_DAYS = 30;
    public static final int QUEUE_SIZE = 100;
    public static final int THREAD_TTL = 120000; // Idle thread lifespan - 2 minutes.

    private final int failedRetryMin;
    private final int jobs;
    private final int maxThreads;
    private final int minThreads;
    private final int pollDbMillis;
    private final boolean pollEnabled;
    private final int purgeJobDays;
    private final List<RunFromPool> runFromPools;
    private final String sendToPool;
    private final int ttl;

    ThreadPool(Element poolElement) throws ServiceConfigException, NumberFormatException {
        String sendToPool = poolElement.getAttribute("send-to-pool").intern();
        if (sendToPool.isEmpty()) {
            throw new ServiceConfigException("<thread-pool> element send-to-pool attribute is empty");
        }
        this.sendToPool = sendToPool;
        String purgeJobDays = poolElement.getAttribute("purge-job-days").intern();
        if (purgeJobDays.isEmpty()) {
            this.purgeJobDays = PURGE_JOBS_DAYS;
        } else {
            try {
                this.purgeJobDays = Integer.parseInt(purgeJobDays);
                if (this.purgeJobDays < 0) {
                    throw new ServiceConfigException("<thread-pool> element purge-job-days attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                throw new ServiceConfigException("<thread-pool> element purge-job-days attribute value is invalid");
            }
        }
        String failedRetryMin = poolElement.getAttribute("failed-retry-min").intern();
        if (failedRetryMin.isEmpty()) {
            this.failedRetryMin = FAILED_RETRY_MIN;
        } else {
            try {
                this.failedRetryMin = Integer.parseInt(failedRetryMin);
                if (this.failedRetryMin < 0) {
                    throw new ServiceConfigException("<thread-pool> element failed-retry-min attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element failed-retry-min attribute value is invalid");
            }
        }
        String ttl = poolElement.getAttribute("ttl").intern();
        if (ttl.isEmpty()) {
            this.ttl = THREAD_TTL;
        } else {
            try {
                this.ttl = Integer.parseInt(ttl);
                if (this.ttl < 0) {
                    throw new ServiceConfigException("<thread-pool> element ttl attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element ttl attribute value is invalid");
            }
        }
        String jobs = poolElement.getAttribute("jobs").intern();
        if (ttl.isEmpty()) {
            this.jobs = QUEUE_SIZE;
        } else {
            try {
                this.jobs = Integer.parseInt(jobs);
                if (this.jobs < 1) {
                    throw new ServiceConfigException("<thread-pool> element jobs attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element jobs attribute value is invalid");
            }
        }
        String minThreads = poolElement.getAttribute("min-threads").intern();
        if (minThreads.isEmpty()) {
            this.minThreads = MIN_THREADS;
        } else {
            try {
                this.minThreads = Integer.parseInt(minThreads);
                if (this.minThreads < 1) {
                    throw new ServiceConfigException("<thread-pool> element min-threads attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element min-threads attribute value is invalid");
            }
        }
        String maxThreads = poolElement.getAttribute("max-threads").intern();
        if (maxThreads.isEmpty()) {
            this.maxThreads = MAX_THREADS;
        } else {
            try {
                this.maxThreads = Integer.parseInt(maxThreads);
                if (this.maxThreads < this.minThreads) {
                    throw new ServiceConfigException("<thread-pool> element max-threads attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element max-threads attribute value is invalid");
            }
        }
        this.pollEnabled = !"false".equals(poolElement.getAttribute("poll-enabled"));
        String pollDbMillis = poolElement.getAttribute("poll-db-millis").intern();
        if (pollDbMillis.isEmpty()) {
            this.pollDbMillis = POLL_WAIT;
        } else {
            try {
                this.pollDbMillis = Integer.parseInt(pollDbMillis);
                if (this.pollDbMillis < 0) {
                    throw new ServiceConfigException("<thread-pool> element poll-db-millis attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element poll-db-millis attribute value is invalid");
            }
        }
        List<? extends Element> runFromPoolElementList = UtilXml.childElementList(poolElement, "run-from-pool");
        if (runFromPoolElementList.isEmpty()) {
            this.runFromPools = Collections.emptyList();
        } else {
            List<RunFromPool> runFromPools = new ArrayList<>(runFromPoolElementList.size());
            for (Element runFromPoolElement : runFromPoolElementList) {
                runFromPools.add(new RunFromPool(runFromPoolElement));
            }
            this.runFromPools = Collections.unmodifiableList(runFromPools);
        }
    }

    public int getFailedRetryMin() {
        return failedRetryMin;
    }

    public int getJobs() {
        return jobs;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public int getPollDbMillis() {
        return pollDbMillis;
    }

    public boolean getPollEnabled() {
        return pollEnabled;
    }

    public int getPurgeJobDays() {
        return purgeJobDays;
    }

    public List<RunFromPool> getRunFromPools() {
        return this.runFromPools;
    }

    public String getSendToPool() {
        return sendToPool;
    }

    public int getTtl() {
        return ttl;
    }
}
