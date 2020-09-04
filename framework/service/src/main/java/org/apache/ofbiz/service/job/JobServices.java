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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.config.ServiceConfigUtil;

public class JobServices {

    private static final String MODULE = JobServices.class.getName();
    private static final String RESOURCE = "ServiceErrorUiLabels";

    public static Map<String, Object> cancelJob(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = ServiceUtil.getLocale(context);

        String jobId = (String) context.get("jobId");
        Map<String, Object> fields = UtilMisc.<String, Object>toMap("jobId", jobId);

        GenericValue job = null;
        try {
            job = EntityQuery.use(delegator).from("JobSandbox").where("jobId", jobId).queryOne();
            if (job != null) {
                job.set("cancelDateTime", UtilDateTime.nowTimestamp());
                job.set("statusId", "SERVICE_CANCELLED");
                job.store();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            String errMsg = UtilProperties.getMessage(RESOURCE, "serviceUtil.unable_to_cancel_job", locale) + " : " + fields;
            return ServiceUtil.returnError(errMsg);
        }

        if (job != null) {
            Timestamp cancelDate = job.getTimestamp("cancelDateTime");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("cancelDateTime", cancelDate);
            result.put("statusId", "SERVICE_PENDING"); // To more easily see current pending jobs and possibly cancel some others
            return result;
        }
        String errMsg = UtilProperties.getMessage(RESOURCE, "serviceUtil.unable_to_cancel_job", locale) + " : " + null;
        return ServiceUtil.returnError(errMsg);
    }

    public static Map<String, Object> cancelJobRetries(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = ServiceUtil.getLocale(context);
        if (!security.hasPermission("SERVICE_INVOKE_ANY", userLogin)) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "serviceUtil.no_permission_to_run", locale) + ".";
            return ServiceUtil.returnError(errMsg);
        }

        String jobId = (String) context.get("jobId");
        Map<String, Object> fields = UtilMisc.<String, Object>toMap("jobId", jobId);

        GenericValue job = null;
        try {
            job = EntityQuery.use(delegator).from("JobSandbox").where("jobId", jobId).queryOne();
            if (job != null) {
                job.set("maxRetry", 0L);
                job.store();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            String errMsg = UtilProperties.getMessage(RESOURCE, "serviceUtil.unable_to_cancel_job_retries", locale) + " : " + fields;
            return ServiceUtil.returnError(errMsg);
        }

        if (job != null) {
            return ServiceUtil.returnSuccess();
        }
        String errMsg = UtilProperties.getMessage(RESOURCE, "serviceUtil.unable_to_cancel_job_retries", locale) + " : " + null;
        return ServiceUtil.returnError(errMsg);
    }

    public static Map<String, Object> purgeOldJobs(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        String sendPool = (String) context.get("poolId");
        Integer daysToKeep = (Integer) context.get("daysToKeep");
        Integer limit = (Integer) context.get("limit");
        try {
            if (sendPool == null) sendPool = ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool();
            if (daysToKeep == null) daysToKeep = ServiceConfigUtil.getServiceEngine().getThreadPool().getPurgeJobDays();
            if (limit == null) limit = ServiceConfigUtil.getServiceEngine().getThreadPool().getMaxThreads();
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting service configuration: ", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceExceptionThrownWhileGettingServiceConfiguration",
                    UtilMisc.toMap("errorString", e), locale));
        }
        Delegator delegator = dctx.getDelegator();
        Timestamp purgeTime = Timestamp.from(Instant.now().minus(Duration.ofDays(daysToKeep)));

        // create the conditions to query
        List<EntityCondition> purgeCondition = UtilMisc.toList(
                EntityCondition.makeCondition("poolId", sendPool),
                EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("finishDateTime", EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition("finishDateTime", EntityOperator.LESS_THAN, purgeTime))),
                        EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("cancelDateTime", EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition("cancelDateTime", EntityOperator.LESS_THAN, purgeTime)))),
                        EntityOperator.OR));

        EntityQuery jobQuery = EntityQuery.use(delegator).from("JobSandbox")
                .where(purgeCondition)
                .select("jobId");
        if (limit != null) {
            jobQuery.maxRows(limit);
        }
        try {
            jobQuery.queryList().forEach(JobUtil::removeJob);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> resetJob(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        String jobId = (String) context.get("jobId");
        GenericValue job;
        try {
            job = EntityQuery.use(delegator).from("JobSandbox").where("jobId", jobId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the job
        if (job != null) {
            job.set("statusId", "SERVICE_PENDING");
            job.set("startDateTime", null);
            job.set("finishDateTime", null);
            job.set("cancelDateTime", null);
            job.set("runByInstanceId", null);

            // save the job
            try {
                job.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
