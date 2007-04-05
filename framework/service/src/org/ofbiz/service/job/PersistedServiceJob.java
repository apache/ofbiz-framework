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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.xml.sax.SAXException;

/**
 * Entity Service Job - Store => Schedule => Run
 */
public class PersistedServiceJob extends GenericServiceJob {

    public static final String module = PersistedServiceJob.class.getName();

    private transient GenericDelegator delegator = null;
    private Timestamp storedDate = null;
    private long nextRecurrence = -1;
    private long maxRetry = -1;

    /**
     * Creates a new PersistedServiceJob
     * @param dctx
     * @param jobValue
     * @param req
     */
    public PersistedServiceJob(DispatchContext dctx, GenericValue jobValue, GenericRequester req) {
        super(jobValue.getString("jobId"), jobValue.getString("jobName"));
        this.delegator = dctx.getDelegator();
        this.requester = req;
        this.dctx = dctx;
        this.storedDate = jobValue.getTimestamp("runTime");
        this.runtime = storedDate.getTime();
        this.maxRetry = jobValue.get("maxRetry") != null ? jobValue.getLong("maxRetry").longValue() : -1;
    }

    public void queue() throws InvalidJobException {
        super.queue();

        // refresh the job object
        GenericValue jobValue = null;
        try {
            jobValue = this.getJob();
            jobValue.refresh();
        } catch (GenericEntityException e) {
            runtime = -1;
            throw new InvalidJobException("Unable to refresh Job object", e);
        }

        // make sure it isn't already set/cancelled
        if (runtime != -1) {
            Timestamp cancelTime = jobValue.getTimestamp("cancelDateTime");
            Timestamp startTime = jobValue.getTimestamp("startDateTime");
            if (cancelTime != null || startTime != null) {
                // job not available
                runtime = -1;
                throw new InvalidJobException("Job [" + getJobId() + "] is not available");

            } else {
                // set the start time to now
                jobValue.set("startDateTime", UtilDateTime.nowTimestamp());
                jobValue.set("statusId", "SERVICE_RUNNING");
                try {
                    jobValue.store();
                } catch (GenericEntityException e) {
                    runtime = -1;
                    throw new InvalidJobException("Unable to set the startDateTime on the current job [" + getJobId() + "]; not running!", e);

                }
            }
        }
    }

    /**
     * @see org.ofbiz.service.job.GenericServiceJob#init()
     */
    protected void init() throws InvalidJobException {
        super.init();

        // configure any addition recurrences
        GenericValue job = this.getJob();
        RecurrenceInfo recurrence = JobManager.getRecurrenceInfo(job);

        String instanceId = UtilProperties.getPropertyValue("general.properties", "unique.instanceId", "ofbiz0");
        if (!instanceId.equals(job.getString("runByInstanceId"))) {
            throw new InvalidJobException("Job has been accpeted by a different instance!");
        }

        try {
            if (recurrence != null) {
                recurrence.incrementCurrentCount();
                long next = recurrence.next();
                createRecurrence(job, next);
            }
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (Debug.infoOn()) Debug.logInfo(this.toString() + "[" + getJobId() + "] -- Next runtime: " + nextRecurrence, module);
    }

    private void createRecurrence(GenericValue job, long next) throws GenericEntityException {
        if (Debug.verboseOn()) Debug.logVerbose("Next runtime returned: " + next, module);

        if (next > runtime) {
            String newJobId = job.getDelegator().getNextSeqId("JobSandbox");
            String pJobId = job.getString("parentJobId");
            if (pJobId == null) {
                pJobId = job.getString("jobId");
            }
            GenericValue newJob = GenericValue.create(job);
            newJob.set("jobId", newJobId);
            newJob.set("previousJobId", job.getString("jobId"));
            newJob.set("parentJobId", pJobId);
            newJob.set("statusId", "SERVICE_PENDING");
            newJob.set("startDateTime", null);
            newJob.set("runByInstanceId", null);
            newJob.set("runTime", new java.sql.Timestamp(next));
            nextRecurrence = next;
            delegator.create(newJob);
            if (Debug.verboseOn()) Debug.logVerbose("Created next job entry: " + newJob, module);
        }
    }

    /**
     * @see org.ofbiz.service.job.GenericServiceJob#finish()
     */
    protected void finish() throws InvalidJobException {
        super.finish();

        // set the finish date
        GenericValue job = getJob();
        String status = job.getString("statusId");
        if (status == null || "SERVICE_RUNNING".equals(status)) {
            job.set("statusId", "SERVICE_FINISHED");
        }
        job.set("finishDateTime", UtilDateTime.nowTimestamp());
        try {
            job.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot update the job [" + getJobId() + "] sandbox", module);
        }
    }

    /**
     * @see org.ofbiz.service.job.GenericServiceJob#failed(Throwable)
     */
    protected void failed(Throwable t) throws InvalidJobException {
        super.failed(t);

        GenericValue job = getJob();
        // if the job has not been re-scheduled; we need to re-schedule and run again
        if (nextRecurrence == -1) {
            if (this.canRetry()) {
                // create a recurrence
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.MINUTE, ServiceConfigUtil.getFailedRetryMin());
                long next = cal.getTimeInMillis();
                try {
                    createRecurrence(job, next);
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, "ERROR: Unable to re-schedule job [" + getJobId() + "] to re-run : " + job, module);
                }
                Debug.log("Persisted Job [" + getJobId() + "] Failed Re-Scheduling : " + next, module);
            } else {
                Debug.logWarning("Persisted Job [" + getJobId() + "] Failed - Max Retry Hit; not re-scheduling", module);
            }
        }
        // set the failed status
        job.set("statusId", "SERVICE_FAILED");
        job.set("finishDateTime", UtilDateTime.nowTimestamp());
        try {
            job.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot update the job sandbox", module);
        }
    }

    /**
     * @see org.ofbiz.service.job.GenericServiceJob#getServiceName()
     */
    protected String getServiceName() throws InvalidJobException {
        GenericValue jobObj = getJob();
        if (jobObj == null || jobObj.get("serviceName") == null) {
            return null;
        }
        return jobObj.getString("serviceName");
    }

    /**
     * @see org.ofbiz.service.job.GenericServiceJob#getContext()
     */
    protected Map getContext() throws InvalidJobException {
        Map context = null;
        try {
            GenericValue jobObj = getJob();
            if (!UtilValidate.isEmpty(jobObj.getString("runtimeDataId"))) {
                GenericValue contextObj = jobObj.getRelatedOne("RuntimeData");
                if (contextObj != null) {
                    context = (Map) XmlSerializer.deserialize(contextObj.getString("runtimeInfo"), delegator);
                }
            }

            if (context == null) {
                context = new HashMap();
            }

            // check the runAsUser
            if (!UtilValidate.isEmpty(jobObj.get("runAsUser"))) {
                context.put("userLogin", ServiceUtil.getUserLogin(dctx, context, jobObj.getString("runAsUser")));                
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): Entity Exception", module);
        } catch (SerializeException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): Serialize Exception", module);
        } catch (ParserConfigurationException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): Parse Exception", module);
        } catch (SAXException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): SAXException", module);
        } catch (IOException e) {
            Debug.logError(e, "PersistedServiceJob.getContext(): IOException", module);
        }
        if (context == null) {
            Debug.logError("Job context is null", module);
        }

        return context;
    }

    // gets the job value object
    private GenericValue getJob() throws InvalidJobException {
        try {
            Map fields = UtilMisc.toMap("jobId", getJobId());
            GenericValue jobObj = delegator.findByPrimaryKey("JobSandbox", fields);

            if (jobObj == null) {
                throw new InvalidJobException("Job [" + getJobId() + "] came back null from datasource");
            }
            return jobObj;
        } catch (GenericEntityException e) {
            throw new InvalidJobException("Cannot get job definition [" + getJobId() + "] from entity", e);
        }
    }

    // returns the number of current retries
    private long getRetries() throws InvalidJobException {
        GenericValue job = this.getJob();
        String pJobId = job.getString("parentJobId");
        if (pJobId == null) {
            return 0;
        }

        Map fields = UtilMisc.toMap("parentJobId", pJobId, "statusId", "SERVICE_FAILED");
        long count = 0;
        try {
            count = delegator.findCountByAnd("JobSandbox", fields);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return count + 1; // add one for the parent
    }

    private boolean canRetry() throws InvalidJobException {
        if (maxRetry == -1) {
            return true;
        }
        if (this.getRetries() < maxRetry) {
            return true;
        }
        return false;
    }
}
