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
package org.apache.ofbiz.service.tracker;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.apache.ofbiz.base.util.UtilValidate.isEmpty;

public class JobTrackerFactory {
    private static final UtilCache<String, JobTracker> TRACKER_CACHE = UtilCache.createUtilCache("service.tracker");

    private final String jobTrackerId;
    private final LocalDispatcher dispatcher;
    private final Delegator delegator;
    private final TimeZone timeZone;
    private final Locale locale;
    private final GenericValue userLogin;
    private String serviceName;
    private Map<String, Object> serviceParams;
    private Boolean persistResult;

    public JobTrackerFactory(LocalDispatcher dispatcher, TimeZone timeZone, Locale locale, GenericValue userLogin) {
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        this.timeZone = timeZone;
        this.userLogin = userLogin;
        this.locale = locale;
        jobTrackerId = delegator.getNextSeqId("JobTracker");
    }

    public JobTrackerFactory(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        this.timeZone = TimeZone.getDefault();
        this.userLogin = null;
        this.locale = Locale.getDefault();
        jobTrackerId = delegator.getNextSeqId("JobTracker");
    }

    /**
     * set the service name need to call to populate the jobTracker with followed jobs
     * @param serviceName
     * @return this
     */
    public JobTrackerFactory setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    /**
     * set parameters needs for the called service to populate the jobTracker with followed jobs
     * @param serviceParams
     * @return this
     */
    public JobTrackerFactory setServiceParams(Map<String, Object> serviceParams) {
        this.serviceParams = serviceParams;
        return this;
    }

    /**
     * indicate that we want to persist result for this tracker
     * @return this
     */
    public JobTrackerFactory persistResult() {
        this.persistResult = Boolean.TRUE;
        return this;
    }

    /**
     * indicate if we want to persist result for this tracker
     * @return this
     */
    public JobTrackerFactory persistResult(Boolean persistResult) {
        this.persistResult = persistResult;
        return this;
    }

    /**
     * @param dispatcher
     * @param jobTrackerId
     * @return the jobTracker corresponding to the jobTrackerId
     * @throws GenericEntityException
     */
    public static JobTracker getJobTracker(LocalDispatcher dispatcher, String jobTrackerId) throws GenericEntityException {
        if (UtilValidate.isEmpty(jobTrackerId)) {
            return null;
        }
        JobTracker jobTracker = TRACKER_CACHE.get(jobTrackerId);
        if (jobTracker == null) {
            GenericValue trackerValue = EntityQuery.use(dispatcher.getDelegator()).from("JobTracker")
                    .where("jobTrackerId", jobTrackerId)
                    .queryOne();
            if (trackerValue == null) {
                throw new GenericEntityException("Could not find jobTracker with ID: " + jobTrackerId);
            }
            jobTracker = TRACKER_CACHE.putIfAbsentAndGet(jobTrackerId, new JobTracker(dispatcher, trackerValue));
        }
        return jobTracker;
    }

    /**
     * @return a jobTracker instance with values prepare with the factory
     * @throws GenericServiceException
     * @throws GenericEntityException
     */
    public JobTracker instantiate() throws GenericServiceException, GenericEntityException {
        GenericValue trackerUserLogin;
        if (!isEmpty(this.userLogin)) {
            trackerUserLogin = this.userLogin;
        } else if (!isEmpty(serviceParams.get("userLogin"))) {
            trackerUserLogin = (GenericValue) serviceParams.get("userLogin");
        } else {
            throw new GenericServiceException("No user login found for JobTracker init.");
        }

        JobTracker tracker = new JobTracker(dispatcher, timeZone, locale, trackerUserLogin,
                jobTrackerId, serviceName, serviceParams,
                isEmpty(persistResult) ? Boolean.FALSE : persistResult);
        return TRACKER_CACHE.putIfAbsentAndGet(jobTrackerId, tracker);
    }

    /**
     * clean cache for a jobTracker instance
     */
    public static void clean(String jobTrackerId) {
        TRACKER_CACHE.remove(jobTrackerId);
    }
}
