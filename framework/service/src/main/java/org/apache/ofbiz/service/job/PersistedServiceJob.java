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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericRequester;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.calendar.RecurrenceInfo;
import org.apache.ofbiz.service.calendar.RecurrenceInfoException;
import org.apache.ofbiz.service.calendar.TemporalExpression;
import org.apache.ofbiz.service.calendar.TemporalExpressionWorker;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.xml.sax.SAXException;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

/**
 * A {@link Job} that is backed by the entity engine. Job data is stored
 * in the JobSandbox entity.
 * <p>When the job is queued, this object "owns" the entity value. Any external changes
 * are ignored except the cancelDateTime field - jobs can be canceled after they are queued.</p>
 */
@SuppressWarnings("serial")
public class PersistedServiceJob extends GenericServiceJob {

    private static final String MODULE = PersistedServiceJob.class.getName();

    private final transient Delegator delegator;
    private long nextRecurrence = -1;
    private final long maxRetry;
    private final long currentRetryCount;
    private final GenericValue jobValue;
    private final long startTime;

    /**
     * Creates a new PersistedServiceJob
     * @param dctx
     * @param jobValue
     * @param req
     */
    public PersistedServiceJob(DispatchContext dctx, GenericValue jobValue, GenericRequester req) {
        super(dctx, jobValue.getString("jobId"), jobValue.getString("jobName"), null, null, req);
        this.delegator = dctx.getDelegator();
        this.jobValue = jobValue;
        Timestamp storedDate = jobValue.getTimestamp("runTime");
        this.startTime = storedDate.getTime();
        this.maxRetry = jobValue.get("maxRetry") != null ? jobValue.getLong("maxRetry") : 0;
        Long retryCount = jobValue.getLong("currentRetryCount");
        if (retryCount != null) {
            this.currentRetryCount = retryCount;
        } else {
            // backward compatibility
            this.currentRetryCount = getRetries(this.delegator);
        }
    }

    @Override
    public void queue() throws InvalidJobException {
        super.queue();
        try {
            jobValue.refresh();
        } catch (GenericEntityException e) {
            throw new InvalidJobException("Unable to refresh JobSandbox value", e);
        }
        if (!JobManager.INSTANCE_ID.equals(jobValue.getString("runByInstanceId"))) {
            throw new InvalidJobException("Job has been accepted by a different instance");
        }
        Timestamp cancelTime = jobValue.getTimestamp("cancelDateTime");
        Timestamp startTime = jobValue.getTimestamp("startDateTime");
        if (cancelTime != null || startTime != null) {
            // job not available
            throw new InvalidJobException("Job [" + getJobId() + "] is not available");
        }
        jobValue.set("statusId", "SERVICE_QUEUED");
        try {
            jobValue.store();
        } catch (GenericEntityException e) {
            throw new InvalidJobException("Unable to set the startDateTime and statusId on the current job [" + getJobId() + "]; not running!", e);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Placing job [" + getJobId() + "] in queue", MODULE);
        }
    }

    @Override
    protected void init() throws InvalidJobException {
        super.init();
        try {
            jobValue.refresh();
        } catch (GenericEntityException e) {
            throw new InvalidJobException("Unable to refresh JobSandbox value", e);
        }
        if (!JobManager.INSTANCE_ID.equals(jobValue.getString("runByInstanceId"))) {
            throw new InvalidJobException("Job has been accepted by a different instance");
        }
        if (jobValue.getTimestamp("cancelDateTime") != null) {
            // Job cancelled
            throw new InvalidJobException("Job [" + getJobId() + "] was cancelled");
        }
        jobValue.set("startDateTime", UtilDateTime.nowTimestamp());
        jobValue.set("statusId", "SERVICE_RUNNING");
        try {
            jobValue.store();
        } catch (GenericEntityException e) {
            throw new InvalidJobException("Unable to set the startDateTime and statusId on the current job [" + getJobId() + "]; not running!", e);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Job [" + getJobId() + "] running", MODULE);
        }
        // configure any additional recurrences
        long maxRecurrenceCount = -1;
        long currentRecurrenceCount = 0;
        TemporalExpression expr = null;
        RecurrenceInfo recurrence = getRecurrenceInfo();
        if (recurrence != null) {
            Debug.logWarning("Persisted Job [" + getJobId() + "] references a RecurrenceInfo, recommend using TemporalExpression instead", MODULE);
            currentRecurrenceCount = recurrence.getCurrentCount();
            expr = RecurrenceInfo.toTemporalExpression(recurrence);
        }
        if (expr == null && UtilValidate.isNotEmpty(jobValue.getString("tempExprId"))) {
            try {
                expr = TemporalExpressionWorker.getTemporalExpression(this.delegator, jobValue.getString("tempExprId"));
            } catch (GenericEntityException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        if (jobValue.get("maxRecurrenceCount") != null) {
            maxRecurrenceCount = jobValue.getLong("maxRecurrenceCount");
        }
        if (jobValue.get("currentRecurrenceCount") != null) {
            currentRecurrenceCount = jobValue.getLong("currentRecurrenceCount");
        }
        if (maxRecurrenceCount != -1) {
            currentRecurrenceCount++;
            jobValue.set("currentRecurrenceCount", currentRecurrenceCount);
        }
        try {
            if (expr != null && (maxRecurrenceCount == -1 || currentRecurrenceCount <= maxRecurrenceCount)) {
                if (recurrence != null) {
                    recurrence.incrementCurrentCount();
                }
                TimeZone timeZone = jobValue.get("recurrenceTimeZone") != null ? TimeZone.getTimeZone(jobValue.getString("recurrenceTimeZone"))
                        : TimeZone.getDefault();
                Calendar next = expr.next(Calendar.getInstance(timeZone));

                if (next != null) {
                    createRecurrence(next.getTimeInMillis(), false);
                }
            }
        } catch (GenericEntityException e) {
            throw new InvalidJobException(e);
        }
        if (Debug.infoOn()) {
            Debug.logInfo("Job  [" + getJobName() + "] Id [" + getJobId() + "] -- Next runtime: " + new Date(nextRecurrence), MODULE);
        }
    }

    private void createRecurrence(long next, boolean isRetryOnFailure) throws GenericEntityException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Next runtime returned: " + next, MODULE);
        }
        if (next > startTime) {
            String pJobId = jobValue.getString("parentJobId");
            if (pJobId == null) {
                pJobId = jobValue.getString("jobId");
            }
            GenericValue newJob = GenericValue.create(jobValue);
            newJob.remove("jobId");
            newJob.set("previousJobId", jobValue.getString("jobId"));
            newJob.set("parentJobId", pJobId);
            newJob.set("statusId", "SERVICE_PENDING");
            newJob.set("startDateTime", null);
            newJob.set("runByInstanceId", null);
            newJob.set("runTime", new java.sql.Timestamp(next));
            if (isRetryOnFailure) {
                newJob.set("currentRetryCount", currentRetryCount + 1);
            } else {
                newJob.set("currentRetryCount", 0L);
            }
            nextRecurrence = next;
            // Set priority if missing
            if (newJob.getLong("priority") == null) {
                newJob.set("priority", JobPriority.NORMAL);
            }
            delegator.createSetNextSeqId(newJob);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created next job entry: " + newJob, MODULE);
            }
        }
    }

    @Override
    protected void finish(Map<String, Object> result) throws InvalidJobException {
        super.finish(result);
        // set the finish date
        jobValue.set("statusId", "SERVICE_FINISHED");
        jobValue.set("finishDateTime", UtilDateTime.nowTimestamp());
        String jobResult = null;
        if (ServiceUtil.isError(result)) {
            jobResult = StringUtils.substring(ServiceUtil.getErrorMessage(result), 0, 255);
        } else {
            jobResult = StringUtils.substring(ServiceUtil.makeSuccessMessage(result, "", "", "", ""), 0, 255);
        }
        if (UtilValidate.isNotEmpty(jobResult)) {
            jobValue.set("jobResult", jobResult);
        }
        try {
            jobValue.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot update the job [" + getJobId() + "] sandbox", MODULE);
        }
    }

    @Override
    protected void failed(Throwable t) throws InvalidJobException {
        super.failed(t);
        // if the job has not been re-scheduled; we need to re-schedule and run again
        if (nextRecurrence == -1) {
            if (this.canRetry()) {
                // create a recurrence
                Calendar cal = Calendar.getInstance();
                try {
                    cal.add(Calendar.MINUTE, ServiceConfigUtil.getServiceEngine().getThreadPool().getFailedRetryMin());
                } catch (GenericConfigException e) {
                    Debug.logWarning(e, "Unable to get retry minutes for job [" + getJobId() + "], defaulting to now: ", MODULE);
                }
                long next = cal.getTimeInMillis();
                try {
                    createRecurrence(next, true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to re-schedule job [" + getJobId() + "]: ", MODULE);
                }
                Debug.logInfo("Persisted Job [" + getJobId() + "] Failed. Re-Scheduling : " + next, MODULE);
            } else {
                Debug.logWarning("Persisted Job [" + getJobId() + "] Failed. Max Retry Hit, not re-scheduling", MODULE);
            }
        }
        // set the failed status
        jobValue.set("statusId", "SERVICE_FAILED");
        jobValue.set("finishDateTime", UtilDateTime.nowTimestamp());
        jobValue.set("jobResult", StringUtils.substring(t.getMessage(), 0, 255));
        try {
            jobValue.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot update the JobSandbox entity", MODULE);
        }
    }

    @Override
    protected String getServiceName() {
        if (jobValue == null || jobValue.get("serviceName") == null) {
            return null;
        }
        return jobValue.getString("serviceName");
    }

    @Override
    protected Map<String, Object> getContext() throws InvalidJobException {
        Map<String, Object> context = null;
        try {
            if (UtilValidate.isNotEmpty(jobValue.getString("runtimeDataId"))) {
                GenericValue contextObj = jobValue.getRelatedOne("RuntimeData", false);
                if (contextObj != null) {
                    context = UtilGenerics.checkMap(XmlSerializer.deserialize(contextObj.getString("runtimeInfo"),
                            delegator), String.class, Object.class);
                }
            }
            if (context == null) {
                context = new HashMap<>();
            }
            // check the runAsUser
            if (UtilValidate.isNotEmpty(jobValue.getString("runAsUser"))) {
                context.put("userLogin", ServiceUtil.getUserLogin(getDctx(), context, jobValue.getString("runAsUser")));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): Entity Exception", MODULE);
        } catch (SerializeException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): Serialize Exception", MODULE);
        } catch (ParserConfigurationException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): Parse Exception", MODULE);
        } catch (SAXException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): SAXException", MODULE);
        } catch (IOException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): IOException", MODULE);
        }
        if (context == null) {
            Debug.logError("Job context is null", MODULE);
        }
        return context;
    }

    // returns the number of current retries
    private long getRetries(Delegator delegator) {
        String pJobId = jobValue.getString("parentJobId");
        if (pJobId == null) {
            return 0;
        }
        long count = 0;
        try {
            count = EntityQuery.use(delegator).from("JobSandbox").where("parentJobId", pJobId, "statusId", "SERVICE_FAILED").queryCount();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Exception thrown while counting retries: ", MODULE);
        }
        return count + 1; // add one for the parent
    }

    private boolean canRetry() {
        if (maxRetry == -1) {
            return true;
        }
        return currentRetryCount < maxRetry;
    }

    private RecurrenceInfo getRecurrenceInfo() {
        try {
            if (UtilValidate.isNotEmpty(jobValue.getString("recurrenceInfoId"))) {
                GenericValue ri = jobValue.getRelatedOne("RecurrenceInfo", false);
                if (ri != null) {
                    return new RecurrenceInfo(ri);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting RecurrenceInfo entity from JobSandbox", MODULE);
        } catch (RecurrenceInfoException re) {
            Debug.logError(re, "Problem creating RecurrenceInfo instance: " + re.getMessage(), MODULE);
        }
        return null;
    }

    @Override
    public void deQueue() throws InvalidJobException {
        if (getCurrentState() != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
        setCurrentState(State.CREATED);
        try {
            jobValue.refresh();
            jobValue.set("startDateTime", null);
            jobValue.set("runByInstanceId", null);
            jobValue.set("statusId", "SERVICE_PENDING");
            jobValue.store();
        } catch (GenericEntityException e) {
            throw new InvalidJobException("Unable to dequeue job [" + getJobId() + "]", e);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Job [" + getJobId() + "] not queued, rescheduling", MODULE);
        }
    }

    @Override
    public Date getStartTime() {
        return new Date(startTime);
    }

    /*
     * Returns the priority stored in the JobSandbox.priority field, if no value is present
     * then it defaults to AbstractJob.getPriority()
     */
    @Override
    public long getPriority() {
        Long priority = jobValue.getLong("priority");
        if (priority == null) {
            return super.getPriority();
        }
        return priority;
    }
}
