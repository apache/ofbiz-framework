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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericRequester;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.semaphore.SemaphoreFailException;
import org.apache.ofbiz.service.semaphore.SemaphoreWaitException;
import org.apache.ofbiz.service.tracker.JobTracker;
import org.apache.ofbiz.service.tracker.JobTrackerListener;

/**
 * A generic async-service job.
 */
@SuppressWarnings("serial")
public class GenericServiceJob extends AbstractJob implements Serializable {

    private static final String MODULE = GenericServiceJob.class.getName();

    private final transient GenericRequester requester;
    private final transient DispatchContext dctx;
    private final String service;
    private final Map<String, Object> context;
    private final JobTracker jobTracker;
    /**
     * Gets dctx.
     * @return the dctx
     */
    public DispatchContext getDctx() {
        return dctx;
    }

    public GenericServiceJob(DispatchContext dctx, String jobId, String jobName, String service, Map<String, Object> context,
                             GenericRequester req, JobTracker jobTracker) {
        super(jobId, jobName);
        Assert.notNull("dctx", dctx);
        this.dctx = dctx;
        this.service = service;
        this.context = context;
        this.requester = req;
        this.jobTracker = jobTracker;
    }

    /**
     * Invokes the service.
     */
    @Override
    public void exec() throws InvalidJobException {
        try {
            refreshStatus();
        } catch (GenericEntityException ignored) {
        }
        if (getCurrentState() == State.ON_HOLD) {
            deQueue();
            return;
        }
        if (getCurrentState() != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
        setCurrentState(State.RUNNING);
        init();
        Throwable thrown = null;
        Map<String, Object> result = null;
        // no transaction is necessary since runSync handles this
        try {
            // get the dispatcher and invoke the service via runSync -- will run all ECAs
            LocalDispatcher dispatcher = dctx.getDispatcher();
            result = dispatcher.runSync(getServiceName(), getContext());
            // check for a failure
            if (ServiceUtil.isError(result)) {
                thrown = new Exception(ServiceUtil.getErrorMessage(result));
            }
            if (requester != null) {
                requester.receiveResult(result);
            }
        } catch (Throwable t) {
            if (requester != null) {
                // pass the exception back to the requester.
                requester.receiveThrowable(t);
            }
            thrown = t;
        }
        if (thrown == null) {
            finish(result);
        } else {
            failed(thrown);
        }
        handleTrackerActions(result);
    }

    /**
     * @param result
     */
    void handleTrackerActions(Map<String, Object> result) {
        Delegator delegator = this.dctx.getDelegator();
        if (UtilValidate.isEmpty(this.jobTracker) || !this.jobTracker.getPersistResult()) {
            return;
        }
        try {
            JobTrackerListener jtl = new JobTrackerListener(delegator, this.jobTracker);
            if (jtl.isFinished()) {
                jobTracker.complete();
            }
            if (!this.jobTracker.getPersistResult()) {
                return;
            }
            Map<String, Object> contextAndResult = new HashMap<>(getContext());
            contextAndResult.putAll(result);
            Map<String, Object> createResultContext = dctx.makeValidContext("createTrackedJobResult",
                    ModelService.IN_PARAM, contextAndResult);
            createResultContext.put("jobTrackerId", this.jobTracker.getJobTrackerId());
            createResultContext.put("jobTrackerResultSeqId", this.getJobId());
            createResultContext.put("userLogin", this.jobTracker.getUserLogin());
            if (ServiceUtil.isError(result) || ServiceUtil.isFailure(result)) {
                createResultContext.put("resultMessage", ServiceUtil.makeErrorMessage(result, "", "", "", ""));
                createResultContext.put("resultCode", ServiceUtil.isError(result) ? ModelService.RESPOND_ERROR : ModelService.RESPOND_FAIL);
            } else {
                createResultContext.put("resultMessage", ServiceUtil.makeSuccessMessage(result, "", "", "", ""));
                createResultContext.put("resultCode", ModelService.RESPOND_SUCCESS);
            }
            dctx.getDispatcher().runSync("createTrackedJobResult", createResultContext);
        } catch (GenericServiceException | InvalidJobException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws GenericEntityException
     */
    protected void refreshStatus() throws GenericEntityException {
        GenericValue job = dctx.getDelegator().findOne("JobSandbox", false, "jobId", getJobId());
        if (job != null) {
            GenericValue statusItem = dctx.getDelegator().findOne("StatusItem", true, "statusId", job.get("statusId"));
            if (currentState() != State.valueOf(statusItem.getString("statusCode"))) {
                setCurrentState(State.valueOf(statusItem.getString("statusCode")));
            }
        }
    }

    /**
     * Method is called prior to running the service.
     */
    protected void init() throws InvalidJobException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Async-Service initializing.", MODULE);
        }
    }

    /**
     * Method is called after the service has finished successfully.
     */
    protected void finish(Map<String, Object> result) throws InvalidJobException {
        if (getCurrentState() != State.RUNNING) {
            throw new InvalidJobException("Illegal state change");
        }
        setCurrentState(State.FINISHED);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Async-Service finished.", MODULE);
        }
    }

    /**
     * Method is called when the service fails.
     * @param t Throwable
     */
    protected void failed(Throwable t) throws InvalidJobException {
        if (t instanceof SemaphoreWaitException || t instanceof SemaphoreFailException) {
            Debug.logError("Async-Service failed due to " + t, MODULE);
        } else {
            Debug.logError(t, "Async-Service failed.", MODULE);
        }
        setCurrentState(State.FAILED);
    }

    /**
     * Gets the context for the service invocation.
     * @return Map of name value pairs making up the service context.
     */
    protected Map<String, Object> getContext() throws InvalidJobException {
        return context;
    }

    /**
     * Gets the name of the service as defined in the definition file.
     * @return The name of the service to be invoked.
     */
    protected String getServiceName() {
        return service;
    }

    @Override
    public boolean isValid() {
        return getCurrentState() == State.CREATED;
    }

    @Override
    public void deQueue() throws InvalidJobException {
        super.deQueue();
        throw new InvalidJobException("Unable to queue job [" + getJobId() + "]");
    }
}
