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

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.config.ConfigHelper;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;thread-pool&gt;</code> element.
 */
@ThreadSafe
public final class ThreadPool extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "thread-pool";
    private final String xPath;

    private static final String MODULE = ThreadPool.class.getName();

    public static final int FAILED_RETRY_MIN = 30;
    public static final int MIN_THREADS = 1; // Must be no less than one or the executor will shut down.
    public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() + 1;
    // See https://stackoverflow.com/questions/13834692/threads-configuration-based-on-no-of-cpu-cores for more
    public static final int POLL_WAIT = 30000; // Database polling interval - 30 seconds.
    public static final int PURGE_JOBS_DAYS = 30;
    public static final int QUEUE_SIZE = 100;
    public static final int THREAD_TTL = 120000; // Idle thread lifespan - 2 minutes.
    public static final int LEASE_REFRESH_MILLIS = 300000; // Heartbeat interval - 5 minutes.
    public static final int LEASE_VALIDATION_MILLIS = 480000; // Stale-job scan interval - 8 minutes.
    public static final int LEASE_EXPIRY_MILLIS = 600000; // Lease expiry threshold - 10 minutes.

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
    private final int leaseRefreshMillis;
    private final int leaseValidationMillis;
    private final int leaseExpiryMillis;

    ThreadPool(Element poolElement, String xPathParent) throws ServiceConfigException, NumberFormatException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        xPath = xPathParent.concat("/thread-pool");
        String sendToPool = config.getValue(xPath.concat("/@send-to-pool"));
        if (sendToPool.isEmpty() && checkStructure) {
            throw new ServiceConfigException("<thread-pool> element send-to-pool attribute is empty");
        }
        this.sendToPool = sendToPool;
        String purgeJobDays = config.getValue(xPath.concat("/@purge-job-days"));
        if (purgeJobDays.isEmpty()) {
            this.purgeJobDays = PURGE_JOBS_DAYS;
        } else {
            try {
                this.purgeJobDays = Integer.parseInt(purgeJobDays);
                if (this.purgeJobDays < 0 && checkStructure) {
                    throw new ServiceConfigException("<thread-pool> element purge-job-days attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                throw new ServiceConfigException("<thread-pool> element purge-job-days attribute value is invalid");
            }
        }
        String failedRetryMin = config.getValue(xPath.concat("/@failed-retry-min"));
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
        String ttl = config.getValue(xPath.concat("/@ttl"));
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
        String jobs = config.getValue(xPath.concat("/@jobs"));
        if (ttl.isEmpty()) {
            this.jobs = QUEUE_SIZE;
        } else {
            try {
                this.jobs = Integer.parseInt(jobs);
                if (this.jobs < 1 && checkStructure) {
                    throw new ServiceConfigException("<thread-pool> element jobs attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element jobs attribute value is invalid");
            }
        }
        String minThreads = config.getValue(xPath.concat("/@min-threads"));
        if (minThreads.isEmpty()) {
            this.minThreads = MIN_THREADS;
        } else {
            try {
                this.minThreads = Integer.parseInt(minThreads);
                if (this.minThreads < 1 && checkStructure) {
                    throw new ServiceConfigException("<thread-pool> element min-threads attribute value is invalid");
                }
            } catch (NumberFormatException | ServiceConfigException e) {
                Debug.logError(e, MODULE);
                throw new ServiceConfigException("<thread-pool> element min-threads attribute value is invalid");
            }
        }
        String maxThreads = config.getValue(xPath.concat("/@max-threads"));
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
        this.pollEnabled = "true".equals(config.getValue(xPath.concat("/@poll-enabled")));
        String pollDbMillis = config.getValue(xPath.concat("/@poll-db-millis"));
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
        this.runFromPools = config.getSubElementsAsListEntries(xPath, poolElement, RunFromPool.class);

        String leaseRefreshMillis = config.getValue(xPath.concat("/@lease-refresh-millis"));
        try {
            this.leaseRefreshMillis = leaseRefreshMillis.isEmpty()
                    ? LEASE_REFRESH_MILLIS
                    : Integer.parseInt(leaseRefreshMillis);
        } catch (NumberFormatException e) {
            Debug.logError(e, MODULE);
            throw new ServiceConfigException("<thread-pool> element lease-refresh-millis attribute value is invalid");
        }
        try {
            String leaseValidationMillis = config.getValue(xPath.concat("/@lease-validation-millis"));
            this.leaseValidationMillis = leaseValidationMillis.isEmpty()
                    ? LEASE_VALIDATION_MILLIS
                    : Integer.parseInt(leaseValidationMillis);
        } catch (NumberFormatException e) {
            Debug.logError(e, MODULE);
            throw new ServiceConfigException("<thread-pool> element lease-validation-millis attribute value is invalid");
        }
        try {
            String leaseExpiryMillis = config.getValue(xPath.concat("/@lease-expiry-millis"));
            this.leaseExpiryMillis = leaseExpiryMillis.isEmpty()
                    ? LEASE_EXPIRY_MILLIS
                    : Integer.parseInt(leaseExpiryMillis);
        } catch (NumberFormatException e) {
            Debug.logError(e, MODULE);
            throw new ServiceConfigException("<thread-pool> element lease-expiry-millis attribute value is invalid");
        }
    }

    ThreadPool(Map<String, Object> configObject, String xPath) throws ServiceConfigException, NumberFormatException {
        this.xPath = xPath;
        String sendToPool = config.getValue(configObject, "/@send-to-pool");
        if (sendToPool.isEmpty()) {
            throw new ServiceConfigException("<thread-pool> element send-to-pool attribute is empty");
        }
        this.sendToPool = sendToPool;
        String purgeJobDays = config.getValue(configObject, "/@purge-job-days");
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
        String failedRetryMin = config.getValue(configObject, "/@failed-retry-min");
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
        String ttl = config.getValue(configObject, "/@ttl");
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
        String jobs = config.getValue(configObject, "/@jobs");
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
        String minThreads = config.getValue(configObject, "/@min-threads");
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
        String maxThreads = config.getValue(configObject, "/@max-threads");
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
        this.pollEnabled = "true".equals(config.getValue(configObject, "/@poll-enabled"));
        String pollDbMillis = config.getValue(configObject, "/@poll-db-millis");
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
        this.runFromPools = config.getSubElementsAsListEntries(xPath, null, RunFromPool.class);

        String leaseRefreshMillis = config.getValue(xPath.concat("/@lease-refresh-millis"));
        try {
            this.leaseRefreshMillis = leaseRefreshMillis.isEmpty()
                    ? LEASE_REFRESH_MILLIS
                    : Integer.parseInt(leaseRefreshMillis);
        } catch (NumberFormatException e) {
            Debug.logError(e, MODULE);
            throw new ServiceConfigException("<thread-pool> element lease-refresh-millis attribute value is invalid");
        }
        try {
            String leaseValidationMillis = config.getValue(xPath.concat("/@lease-validation-millis"));
            this.leaseValidationMillis = leaseValidationMillis.isEmpty()
                    ? LEASE_VALIDATION_MILLIS
                    : Integer.parseInt(leaseValidationMillis);
        } catch (NumberFormatException e) {
            Debug.logError(e, MODULE);
            throw new ServiceConfigException("<thread-pool> element lease-validation-millis attribute value is invalid");
        }
        try {
            String leaseExpiryMillis = config.getValue(xPath.concat("/@lease-expiry-millis"));
            this.leaseExpiryMillis = leaseExpiryMillis.isEmpty()
                    ? LEASE_EXPIRY_MILLIS
                    : Integer.parseInt(leaseExpiryMillis);
        } catch (NumberFormatException e) {
            Debug.logError(e, MODULE);
            throw new ServiceConfigException("<thread-pool> element lease-expiry-millis attribute value is invalid");
        }
    }

    public static ThreadPool loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new ThreadPool(element, xPathParent);
    }

    public static ThreadPool loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new ThreadPool(configMap, xPath);
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
        return runFromPools;
    }

    public String getSendToPool() {
        return sendToPool;
    }

    public int getTtl() {
        return ttl;
    }

    public int getLeaseRefreshMillis() {
        return leaseRefreshMillis;
    }

    public int getLeaseValidationMillis() {
        return leaseValidationMillis;
    }

    public int getLeaseExpiryMillis() {
        return leaseExpiryMillis;
    }

    @Override
    public String getName() {
        return "thread-pool";
    }
}
