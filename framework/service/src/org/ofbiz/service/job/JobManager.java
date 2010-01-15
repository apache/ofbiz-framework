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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.calendar.RecurrenceInfoException;
import org.ofbiz.service.config.ServiceConfigUtil;

/**
 * JobManager
 */
public class JobManager {

    public static final String instanceId = UtilProperties.getPropertyValue("general.properties", "unique.instanceId", "ofbiz0");
    public static final Map<String, Object> updateFields = UtilMisc.<String, Object>toMap("runByInstanceId", instanceId, "statusId", "SERVICE_QUEUED");
    public static final String module = JobManager.class.getName();
    public static final String dispatcherName = "JobDispatcher";
    public static Map<String, JobManager> registeredManagers = FastMap.newInstance();

    protected Delegator delegator;
    protected JobPoller jp;

    /** Creates a new JobManager object. */
    public JobManager(Delegator delegator) {
        this(delegator, true);
    }

    public JobManager(Delegator delegator, boolean enabled) {
        if (delegator == null) {
            throw new GeneralRuntimeException("ERROR: null delegator passed, cannot create JobManager");
        }
        if (JobManager.registeredManagers.get(delegator.getDelegatorName()) != null) {
            throw new GeneralRuntimeException("JobManager for [" + delegator.getDelegatorName() + "] already running");
        }

        this.delegator = delegator;
        jp = new JobPoller(this, enabled);
        JobManager.registeredManagers.put(delegator.getDelegatorName(), this);
    }

    public static JobManager getInstance(Delegator delegator, boolean enabled)
    {
        JobManager jm = JobManager.registeredManagers.get(delegator.getDelegatorName());
        if (jm == null) {
            jm = new JobManager(delegator, enabled);
        }
        return jm;
    }

    /** Queues a Job to run now. */
    public void runJob(Job job) throws JobManagerException {
        if (job.isValid()) {
            jp.queueNow(job);
        }
    }

    /** Returns the ServiceDispatcher. */
    public LocalDispatcher getDispatcher() {
        LocalDispatcher thisDispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);
        return thisDispatcher;
    }

    /** Returns the Delegator. */
    public Delegator getDelegator() {
        return this.delegator;
    }

    public synchronized List<Job> poll() {
        List<Job> poll = FastList.newInstance();

        // sort the results by time
        List<String> order = UtilMisc.toList("runTime");

        // basic query
        List<EntityExpr> expressions = UtilMisc.toList(EntityCondition.makeCondition("runTime", EntityOperator.LESS_THAN_EQUAL_TO,
                UtilDateTime.nowTimestamp()), EntityCondition.makeCondition("startDateTime", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition("cancelDateTime", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition("runByInstanceId", EntityOperator.EQUALS, null));

        // limit to just defined pools
        List<String> pools = ServiceConfigUtil.getRunPools();
        List<EntityExpr> poolsExpr = UtilMisc.toList(EntityCondition.makeCondition("poolId", EntityOperator.EQUALS, null));
        if (pools != null) {
            for (String poolName: pools) {
                poolsExpr.add(EntityCondition.makeCondition("poolId", EntityOperator.EQUALS, poolName));
            }
        }

        // make the conditions
        EntityCondition baseCondition = EntityCondition.makeCondition(expressions);
        EntityCondition poolCondition = EntityCondition.makeCondition(poolsExpr, EntityOperator.OR);
        EntityCondition mainCondition = EntityCondition.makeCondition(UtilMisc.toList(baseCondition, poolCondition));

        // we will loop until we have no more to do
        boolean pollDone = false;

        while (!pollDone) {
            // an extra protection for synchronization, help make sure we don't get in here more than once
            synchronized (this) {
                boolean beganTransaction = false;

                try {
                    beganTransaction = TransactionUtil.begin();
                    if (!beganTransaction) {
                        Debug.logError("Unable to poll for jobs; transaction was not started by this process", module);
                        return null;
                    }

                    List<Job> localPoll = FastList.newInstance();

                    // first update the jobs w/ this instance running information
                    delegator.storeByCondition("JobSandbox", updateFields, mainCondition);

                    // now query all the 'queued' jobs for this instance
                    List<GenericValue> jobEnt = delegator.findByAnd("JobSandbox", updateFields, order);
                    //jobEnt = delegator.findByCondition("JobSandbox", mainCondition, null, order);

                    if (UtilValidate.isNotEmpty(jobEnt)) {
                        for (GenericValue v: jobEnt) {
                            DispatchContext dctx = getDispatcher().getDispatchContext();
                            if (dctx == null) {
                                Debug.logError("Unable to locate DispatchContext object; not running job!", module);
                                continue;
                            }
                            Job job = new PersistedServiceJob(dctx, v, null); // TODO fix the requester
                            try {
                                job.queue();
                                localPoll.add(job);
                            } catch (InvalidJobException e) {
                                Debug.logError(e, module);
                            }
                        }
                    } else {
                        pollDone = true;
                    }

                    // nothing should go wrong at this point, so add to the general list
                    poll.addAll(localPoll);
                } catch (Throwable t) {
                    // catch Throwable so nothing slips through the cracks... this is a fairly sensitive operation
                    String errMsg = "Error in polling JobSandbox: [" + t.toString() + "]. Rolling back transaction.";
                    Debug.logError(t, errMsg, module);
                    try {
                        // only rollback the transaction if we started one...
                        TransactionUtil.rollback(beganTransaction, errMsg, t);
                    } catch (GenericEntityException e2) {
                        Debug.logError(e2, "[Delegator] Could not rollback transaction: " + e2.toString(), module);
                    }
                } finally {
                    try {
                        // only commit the transaction if we started one... but make sure we try
                        TransactionUtil.commit(beganTransaction);
                    } catch (GenericTransactionException e) {
                        String errMsg = "Transaction error trying to commit when polling and updating the JobSandbox: " + e.toString();
                        // we don't really want to do anything different, so just log and move on
                        Debug.logError(e, errMsg, module);
                    }
                }
            }
        }
        return poll;
    }

    public synchronized void reloadCrashedJobs() {
        String instanceId = UtilProperties.getPropertyValue("general.properties", "unique.instanceId", "ofbiz0");
        List<GenericValue> crashed = null;

        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("runByInstanceId", instanceId));
        exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "SERVICE_RUNNING"));
        EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(exprs);

        try {
            crashed = delegator.findList("JobSandbox", ecl, null, UtilMisc.toList("startDateTime"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to load crashed jobs", module);
        }

        if (UtilValidate.isNotEmpty(crashed)) {
            try {
                int rescheduled = 0;
                for (GenericValue job: crashed) {
                    Timestamp now = UtilDateTime.nowTimestamp();
                    Debug.log("Scheduling Job : " + job, module);

                    String pJobId = job.getString("parentJobId");
                    if (pJobId == null) {
                        pJobId = job.getString("jobId");
                    }
                    GenericValue newJob = GenericValue.create(job);
                    newJob.set("statusId", "SERVICE_PENDING");
                    newJob.set("runTime", now);
                    newJob.set("previousJobId", job.getString("jobId"));
                    newJob.set("parentJobId", pJobId);
                    newJob.set("startDateTime", null);
                    newJob.set("runByInstanceId", null);
                    delegator.createSetNextSeqId(newJob);

                    // set the cancel time on the old job to the same as the re-schedule time
                    job.set("statusId", "SERVICE_CRASHED");
                    job.set("cancelDateTime", now);
                    delegator.store(job);

                    rescheduled++;
                }

                if (Debug.infoOn()) Debug.logInfo("-- " + rescheduled + " jobs re-scheduled", module);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

        } else {
            if (Debug.infoOn()) Debug.logInfo("No crashed jobs to re-schedule", module);
        }
    }

    /**
     * Schedule a job to start at a specific time with specific recurrence info
     *@param serviceName The name of the service to invoke
     *@param context The context for the service
     *@param startTime The time in milliseconds the service should run
     *@param frequency The frequency of the recurrence (HOURLY,DAILY,MONTHLY,etc)
     *@param interval The interval of the frequency recurrence
     *@param count The number of times to repeat
     */
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count) throws JobManagerException {
        schedule(serviceName, context, startTime, frequency, interval, count, 0);
    }

    /**
     * Schedule a job to start at a specific time with specific recurrence info
     *@param serviceName The name of the service to invoke
     *@param context The context for the service
     *@param startTime The time in milliseconds the service should run
     *@param frequency The frequency of the recurrence (HOURLY,DAILY,MONTHLY,etc)
     *@param interval The interval of the frequency recurrence
     *@param endTime The time in milliseconds the service should expire
     */
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, long endTime) throws JobManagerException {
        schedule(serviceName, context, startTime, frequency, interval, -1, endTime);
    }

    /**
     * Schedule a job to start at a specific time with specific recurrence info
     *@param serviceName The name of the service to invoke
     *@param context The context for the service
     *@param startTime The time in milliseconds the service should run
     *@param frequency The frequency of the recurrence (HOURLY,DAILY,MONTHLY,etc)
     *@param interval The interval of the frequency recurrence
     *@param count The number of times to repeat
     *@param endTime The time in milliseconds the service should expire
     */
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count, long endTime) throws JobManagerException {
        schedule(null, serviceName, context, startTime, frequency, interval, count, endTime);
    }

    /**
     * Schedule a job to start at a specific time with specific recurrence info
     *@param poolName The name of the pool to run the service from
     *@param serviceName The name of the service to invoke
     *@param context The context for the service
     *@param startTime The time in milliseconds the service should run
     *@param frequency The frequency of the recurrence (HOURLY,DAILY,MONTHLY,etc)
     *@param interval The interval of the frequency recurrence
     *@param count The number of times to repeat
     *@param endTime The time in milliseconds the service should expire
     */
    public void schedule(String poolName, String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count, long endTime) throws JobManagerException {
        schedule(null, null, serviceName, context, startTime, frequency, interval, count, endTime, -1);
    }

    /**
     * Schedule a job to start at a specific time with specific recurrence info
     *@param jobName The name of the job
     *@param poolName The name of the pool to run the service from
     *@param serviceName The name of the service to invoke
     *@param context The context for the service
     *@param startTime The time in milliseconds the service should run
     *@param frequency The frequency of the recurrence (HOURLY,DAILY,MONTHLY,etc)
     *@param interval The interval of the frequency recurrence
     *@param count The number of times to repeat
     *@param endTime The time in milliseconds the service should expire
     *@param maxRetry The max number of retries on failure (-1 for no max)
     */
    public void schedule(String jobName, String poolName, String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count, long endTime, int maxRetry) throws JobManagerException {
        if (delegator == null) {
            Debug.logWarning("No delegator referenced; cannot schedule job.", module);
            return;
        }

        // persist the context
        String dataId = null;
        try {
            GenericValue runtimeData = delegator.makeValue("RuntimeData");
            runtimeData.set("runtimeInfo", XmlSerializer.serialize(context));
            runtimeData = delegator.createSetNextSeqId(runtimeData);
            dataId = runtimeData.getString("runtimeDataId");
        } catch (GenericEntityException ee) {
            throw new JobManagerException(ee.getMessage(), ee);
        } catch (SerializeException se) {
            throw new JobManagerException(se.getMessage(), se);
        } catch (IOException ioe) {
            throw new JobManagerException(ioe.getMessage(), ioe);
        }

        // schedule the job
        schedule(jobName, poolName, serviceName, dataId, startTime, frequency, interval, count, endTime, maxRetry);
    }

    /**
     * Schedule a job to start at a specific time with specific recurrence info
     *@param poolName The name of the pool to run the service from
     *@param serviceName The name of the service to invoke
     *@param dataId The persisted context (RuntimeData.runtimeDataId)
     *@param startTime The time in milliseconds the service should run
     */
    public void schedule(String poolName, String serviceName, String dataId, long startTime) throws JobManagerException {
        schedule(null, poolName, serviceName, dataId, startTime, -1, 0, 1, 0, -1);
    }

    /**
     * Schedule a job to start at a specific time with specific recurrence info
     *@param jobName The name of the job
     *@param poolName The name of the pool to run the service from
     *@param serviceName The name of the service to invoke
     *@param dataId The persisted context (RuntimeData.runtimeDataId)
     *@param startTime The time in milliseconds the service should run
     *@param frequency The frequency of the recurrence (HOURLY,DAILY,MONTHLY,etc)
     *@param interval The interval of the frequency recurrence
     *@param count The number of times to repeat
     *@param endTime The time in milliseconds the service should expire
     *@param maxRetry The max number of retries on failure (-1 for no max)
     */
    public void schedule(String jobName, String poolName, String serviceName, String dataId, long startTime, int frequency, int interval, int count, long endTime, int maxRetry) throws JobManagerException {
        if (delegator == null) {
            Debug.logWarning("No delegator referenced; cannot schedule job.", module);
            return;
        }

        // create the recurrence
        String infoId = null;
        if (frequency > -1 && count != 0) {
            try {
                RecurrenceInfo info = RecurrenceInfo.makeInfo(delegator, startTime, frequency, interval, count);
                infoId = info.primaryKey();
            } catch (RecurrenceInfoException e) {
                throw new JobManagerException(e.getMessage(), e);
            }
        }

        // set the persisted fields
        if (UtilValidate.isEmpty(jobName)) {
            jobName = Long.toString((new Date().getTime()));
        }
        Map<String, Object> jFields = UtilMisc.<String, Object>toMap("jobName", jobName, "runTime", new java.sql.Timestamp(startTime),
                "serviceName", serviceName, "statusId", "SERVICE_PENDING", "recurrenceInfoId", infoId, "runtimeDataId", dataId);

        // set the pool ID
        if (UtilValidate.isNotEmpty(poolName)) {
            jFields.put("poolId", poolName);
        } else {
            jFields.put("poolId", ServiceConfigUtil.getSendPool());
        }

        // set the loader name
        jFields.put("loaderName", dispatcherName);

        // set the max retry
        jFields.put("maxRetry", Long.valueOf(maxRetry));

        // create the value and store
        GenericValue jobV;
        try {
            jobV = delegator.makeValue("JobSandbox", jFields);
            delegator.createSetNextSeqId(jobV);
        } catch (GenericEntityException e) {
            throw new JobManagerException(e.getMessage(), e);
        }
    }

    /**
     * Kill a JobInvoker Thread.
     * @param threadName Name of the JobInvoker Thread to kill.
     */
    public void killThread(String threadName) {
        jp.killThread(threadName);
    }

    /**
     * Get a List of each threads current state.
     * @return List containing a Map of each thread's state.
     */
    public List<Map<String, Object>> processList() {
        return jp.getPoolState();
    }

    /** Close out the scheduler thread. */
    public void shutdown() {
        if (jp != null) {
            jp.stop();
            jp = null;
            Debug.logInfo("JobManager: Stopped Scheduler Thread.", module);
        }
    }

    @Override
    public void finalize() throws Throwable {
        this.shutdown();
        super.finalize();
    }

    /** gets the recurrence info object for a job. */
    public static RecurrenceInfo getRecurrenceInfo(GenericValue job) {
        try {
            if (job != null && !UtilValidate.isEmpty(job.getString("recurrenceInfoId"))) {
                if (job.get("cancelDateTime") != null) {
                    // cancel has been flagged, no more recurrence
                    return null;
                }
                GenericValue ri = job.getRelatedOne("RecurrenceInfo");

                if (ri != null) {
                    return new RecurrenceInfo(ri);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
            Debug.logError(e, "Problem getting RecurrenceInfo entity from JobSandbox", module);
        } catch (RecurrenceInfoException re) {
            re.printStackTrace();
            Debug.logError(re, "Problem creating RecurrenceInfo instance: " + re.getMessage(), module);
        }
        return null;
    }

}
