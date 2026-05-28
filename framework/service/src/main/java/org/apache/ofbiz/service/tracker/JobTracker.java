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

import java.sql.Timestamp;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.apache.ofbiz.base.util.UtilMisc.toList;
import static org.apache.ofbiz.base.util.UtilMisc.toMap;
import static org.apache.ofbiz.entity.condition.EntityCondition.makeCondition;
import static org.apache.ofbiz.entity.condition.EntityOperator.IN;

public class JobTracker {
    private final String trackerId;
    private final LocalDispatcher dispatcher;
    private final Delegator delegator;
    private final String serviceName;
    private final Map<String, Object> processParams;
    private final Map<String, Object> serviceParams;
    private final Boolean persistResult;
    private final GenericValue userLogin;
    private GenericValue jobTrackerGV;
    private String runtimeDataId = null;

    JobTracker(LocalDispatcher dispatcher, TimeZone timeZone, Locale locale,
               GenericValue userLogin, String trackerId, String serviceName, Map<String, Object> parameters,
               Boolean persistResult) throws GenericEntityException {
        this.trackerId = trackerId;
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        this.serviceName = serviceName;
        this.processParams = toMap(
                "jobTrackerId", trackerId,
                "userLogin", userLogin,
                "timeZone", timeZone,
                "locale", locale);
        this.serviceParams = parameters;
        this.serviceParams.putAll(processParams);
        this.jobTrackerGV = EntityQuery.use(delegator).from("JobTracker").where("jobTrackerId", trackerId).queryOne();
        this.persistResult = persistResult;
        this.userLogin = userLogin;
    }

    JobTracker(LocalDispatcher dispatcher, GenericValue jobTracker) throws GenericEntityException {
        if (!"JobTracker".equals(jobTracker.getEntityName())) {
            throw new GenericEntityException("Cannot construct a JobTracker object with a "
                    + jobTracker.getEntityName() + " GenericValue");
        }
        this.trackerId = jobTracker.getString("jobTrackerId");
        this.dispatcher = dispatcher;
        this.delegator = jobTracker.getDelegator();
        this.serviceName = jobTracker.getString("serviceName");
        this.userLogin = delegator.findOne("UserLogin", false,
                "userLoginId", jobTracker.getString("runAsUser"));
        this.processParams = toMap("jobTrackerId", trackerId,
                "userLogin", this.userLogin,
                "timeZone", TimeZone.getDefault(),
                "locale", Locale.getDefault());
        this.runtimeDataId = jobTracker.getString("runtimeDataId");
        this.serviceParams = retrieveServiceParams();
        this.persistResult = jobTracker.getBoolean("persistResult");
        this.jobTrackerGV = jobTracker;
    }

    /**
     * @return the id of a job tracker
     */
    public String getJobTrackerId() {
        return this.trackerId;
    }

    /**
     * @return the GenericValue corresponding to this tracker
     */
    public GenericValue getGenericValue() {
        return jobTrackerGV;
    }

    /**
     * @return true if the jobTracker store the job result after run
     */
    public Boolean getPersistResult() {
        return persistResult;
    }

    /**
     * @return the UserLogin linked
     */
    public GenericValue getUserLogin() {
        return userLogin;
    }

    /**
     * Store the job tracker in database to share it
     *
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public void persist() throws GenericEntityException, GenericServiceException {
        persistParameters();
        Map<String, Object> context = new HashMap<>(processParams);
        context.putAll(serviceParams);
        context.put("serviceName", this.serviceName);
        context.put("runtimeDataId", this.runtimeDataId);
        context.put("persistResult", this.persistResult ? "Y" : "N");
        context.put("runAsUser", this.userLogin.getString("userLoginId"));
        dispatcher.runSync("createJobTracker", context, 60, true);
        this.jobTrackerGV = EntityQuery.use(delegator).from("JobTracker").where("jobTrackerId", trackerId).queryOne();
        if (UtilValidate.isEmpty(this.jobTrackerGV)) {
            throw new GenericEntityException("Couldn't find or create jobTracker GV");
        }
    }

    /**
     * @return the current status of a jobTracker
     * @throws GenericEntityException
     */
    public String getStatusId() throws GenericEntityException {
        if (this.jobTrackerGV == null) {
            return "JOB_T_FAILED";
        }
        jobTrackerGV.refresh();
        return jobTrackerGV.getString("statusId");
    }

    /**
     * Set to pause the jobTracker and all followed jobs
     *
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public void pause() throws GenericEntityException, GenericServiceException {
        delegator.storeByCondition("JobSandbox", toMap("statusId", "SERVICE_ON_HOLD"),
                makeCondition(
                        makeCondition("jobTrackerId", trackerId),
                        makeCondition("statusId", IN, toList("SERVICE_PENDING", "SERVICE_QUEUED"))));
        updateStatus("JOB_T_ON_HOLD");
    }

    /**
     * This restart a paused jobTracker and all followed job
     *
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public void resume() throws GenericEntityException, GenericServiceException {
        delegator.storeByCondition("JobSandbox", toMap("statusId", "SERVICE_PENDING"),
                makeCondition(
                        makeCondition("jobTrackerId", trackerId),
                        makeCondition("statusId", "SERVICE_ON_HOLD")));
        updateStatus("JOB_T_RUNNING");
    }

    /**
     * Stop a jobTracker and all followed job not finished
     *
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public void cancel() throws GenericEntityException, GenericServiceException {
        Timestamp now = UtilDateTime.nowTimestamp();
        delegator.storeByCondition("JobSandbox", toMap("statusId", "SERVICE_CANCELLED",
                        "cancelDateTime", now),
                makeCondition(
                        makeCondition("jobTrackerId", trackerId),
                        makeCondition("statusId", IN, toList("SERVICE_PENDING", "SERVICE_QUEUED", "SERVICE_ON_HOLD"))));
        updateStatus("JOB_T_CANCELLED", toMap("cancelDate", now));
    }

    /**
     * Move the jobTracker to a completed state
     *
     * @throws GenericServiceException
     */
    public void complete() throws GenericServiceException {
        updateStatus("JOB_T_FINISHED", toMap("completionDate", UtilDateTime.nowTimestamp()));
    }

    /**
     * call to update the status of this jobTracker on isolated transaction
     *
     * @param statusId
     * @throws GenericServiceException
     */
    public void updateStatus(String statusId) throws GenericServiceException {
        updateStatus(statusId, null);
    }

    /**
     * call to update the status of this jobTracker on isolated transaction
     *
     * @param statusId
     * @param statusParams
     * @throws GenericServiceException
     */
    public void updateStatus(String statusId, Map<String, Object> statusParams) throws GenericServiceException {
        if (UtilValidate.isEmpty(jobTrackerGV)) {
            return;
        }
        Map<String, Object> updateParameters = new HashMap<>(processParams);
        updateParameters.put("statusId", statusId);
        if (UtilValidate.isNotEmpty(statusParams)) {
            updateParameters.putAll(statusParams);
        }
        dispatcher.runSync("updateJobTracker", updateParameters, 60, true);
    }

    /**
     * compute the number of followed jobs by the tracker after the service to populate them is finished
     *
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public void computeJobsTotalQty() throws GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(jobTrackerGV)) {
            return;
        }
        Map<String, Object> context = new HashMap<>(processParams);
        context.put("jobsTotalQty", EntityQuery.use(delegator)
                .from("JobSandbox")
                .where("jobTrackerId", trackerId)
                .queryCount());
        dispatcher.runSync("updateJobTracker", context, 60, true);
        JobTrackerFactory.clean(trackerId);
    }

    private void persistParameters() throws GenericEntityException {
        this.runtimeDataId = delegator.getNextSeqId("RuntimeData");
        String serializedParams;
        try {
            serviceParams.remove("timeZone"); // unsupport by serializer
            serializedParams = XmlSerializer.serialize(serviceParams);
        } catch (SerializeException | IOException e) {
            throw new RuntimeException(e);
        }
        delegator.create("RuntimeData", toMap(
                "runtimeDataId", runtimeDataId,
                "runtimeInfo", serializedParams));
    }

    private Map<String, Object> retrieveServiceParams() throws GenericEntityException {
        GenericValue runtimeData = delegator
                .findOne("RuntimeData", true, "runtimeDataId", runtimeDataId);
        try {
            return UtilGenerics.checkMap(
                    XmlSerializer.deserialize(runtimeData.getString("runtimeInfo"), delegator), String.class, Object.class);
        } catch (SerializeException | SAXException | ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
